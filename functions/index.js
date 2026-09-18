const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { onSchedule } = require("firebase-functions/v2/scheduler");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");
const { getStorage } = require("firebase-admin/storage");

initializeApp();

const MEDIA_TTL_DAYS = 7;

function previewTextFor(message) {
  if (message.type === "IMAGE") return "📷 Foto";
  if (message.type === "AUDIO") return "🎵 Áudio";
  return message.text || "";
}

/**
 * Dispara quando uma mensagem nova é criada em families/{familyId}/messages/{messageId}.
 * Manda push (via FCM) pra todos os membros da família, exceto quem enviou.
 * Usa "data" payload (não "notification") pra o app decidir como mostrar.
 */
exports.onNewMessage = onDocumentCreated(
  { document: "families/{familyId}/messages/{messageId}", region: "southamerica-east1" },
  async (event) => {
    const message = event.data.data();
    const familyId = event.params.familyId;

    const familySnap = await getFirestore().collection("families").doc(familyId).get();
    if (!familySnap.exists) return;

    const family = familySnap.data();
    const tokens = family.memberTokens || {};
    const senderId = message.senderId;

    const targetTokens = Object.entries(tokens)
      .filter(([uid]) => uid !== senderId)
      .map(([, token]) => token)
      .filter((token) => typeof token === "string" && token.length > 0);

    if (targetTokens.length === 0) return;

    await getMessaging().sendEachForMulticast({
      tokens: targetTokens,
      data: {
        senderName: message.senderName || "Família",
        text: previewTextFor(message),
      },
      android: { priority: "high" },
    });
  }
);

/**
 * Roda todo dia de madrugada e apaga do Storage as fotos/áudios com mais de
 * MEDIA_TTL_DAYS dias. As mensagens de texto nunca são apagadas — só a mídia
 * em si, pra não gastar espaço de armazenamento à toa.
 */
exports.cleanupExpiredMedia = onSchedule(
  { schedule: "every day 03:00", timeZone: "America/Sao_Paulo", region: "southamerica-east1" },
  async () => {
    const db = getFirestore();
    const cutoff = new Date(Date.now() - MEDIA_TTL_DAYS * 24 * 60 * 60 * 1000);
    const bucket = getStorage().bucket();

    const snapshot = await db
      .collectionGroup("messages")
      .where("timestamp", "<", cutoff)
      .get();

    const batch = db.batch();
    let pendingWrites = 0;

    for (const doc of snapshot.docs) {
      const data = doc.data();
      const isExpirableMedia = (data.type === "IMAGE" || data.type === "AUDIO") && !data.expired && data.mediaPath;
      if (!isExpirableMedia) continue;

      await bucket.file(data.mediaPath).delete({ ignoreNotFound: true });
      batch.update(doc.ref, { expired: true, mediaUrl: null, mediaPath: null });
      pendingWrites++;
    }

    if (pendingWrites > 0) await batch.commit();
  }
);
