// Recibe el formulario de contacto del sitio y lo manda a support@email-alarm.com.
//
// Va por Resend (el dominio ya esta verificado ahi) y no por el buzon de Zoho:
// para mandar desde un programa hace falta una clave, y meter la contrasena del
// buzon en el servidor seria regalarle a cualquiera el acceso al correo.
//
// La clave vive en la variable de entorno RESEND_API_KEY de Vercel. Si falta,
// la funcion responde que no esta configurada en vez de romper en silencio.

const DESTINO = "support@email-alarm.com";
// El remitente es del dominio propio, pero NO el buzon real: asi una respuesta
// automatica nunca vuelve a este mismo formulario. Responder va a quien escribio.
const REMITENTE = "Email Alarm <web@email-alarm.com>";

const LARGO_MAXIMO = { nombre: 80, email: 140, mensaje: 4000 };

module.exports = async (req, res) => {
  if (req.method !== "POST") {
    res.setHeader("Allow", "POST");
    return res.status(405).json({ error: "metodo" });
  }

  const cuerpo = typeof req.body === "string" ? parsear(req.body) : req.body || {};
  const nombre = texto(cuerpo.nombre, LARGO_MAXIMO.nombre);
  const email = texto(cuerpo.email, LARGO_MAXIMO.email);
  const mensaje = texto(cuerpo.mensaje, LARGO_MAXIMO.mensaje);

  // Campo invisible: una persona nunca lo ve, un robot lo completa. Se responde
  // que salio bien para no ensenarle al robot que fue descubierto.
  if (texto(cuerpo.web, 200)) return res.status(200).json({ ok: true });

  if (!email || !email.includes("@") || !mensaje) {
    return res.status(400).json({ error: "faltan" });
  }

  const clave = process.env.RESEND_API_KEY;
  if (!clave) return res.status(500).json({ error: "sin_clave" });

  const texto_plano =
    `De: ${nombre || "(sin nombre)"} <${email}>\n` +
    `Idioma: ${texto(cuerpo.idioma, 10) || "?"}\n` +
    `Pagina: ${texto(cuerpo.pagina, 200) || "?"}\n\n` +
    mensaje;

  try {
    const r = await fetch("https://api.resend.com/emails", {
      method: "POST",
      headers: {
        Authorization: `Bearer ${clave}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        from: REMITENTE,
        to: [DESTINO],
        // Responder en el buzon contesta a quien escribio, no al formulario.
        reply_to: email,
        subject: `Contacto del sitio — ${nombre || email}`,
        text: texto_plano,
      }),
    });

    if (!r.ok) {
      const detalle = await r.text();
      console.error("Resend rechazo el envio:", r.status, detalle);
      return res.status(502).json({ error: "envio" });
    }
    return res.status(200).json({ ok: true });
  } catch (e) {
    console.error("No se pudo llamar a Resend:", e);
    return res.status(502).json({ error: "envio" });
  }
};

/** Recorta y limpia: nada de saltos raros ni campos infinitos. */
function texto(valor, maximo) {
  if (typeof valor !== "string") return "";
  return valor.trim().slice(0, maximo);
}

function parsear(cuerpo) {
  try {
    return JSON.parse(cuerpo);
  } catch {
    return {};
  }
}
