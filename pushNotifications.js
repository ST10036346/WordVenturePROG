// VS Code / Node.js / Express
const express = require('express');
const router = express.Router();
const admin = require('firebase-admin');

// 1. INITIALIZE FIREBASE ADMIN SDK 
const serviceAccount = require('./serviceAccountKey.json');

// Initialize the Admin SDK if it hasn't been already
if (!admin.apps.length) {
    admin.initializeApp({
        credential: admin.credential.cert(serviceAccount)
    });
}
const db = admin.firestore();

const verifyAuth = (req, res, next) => {
    console.log("Authorization Check: Passing for development.");
    next(); 
};

//ENDPOINT A: REGISTER/UPDATE FCM TOKEN 
router.patch('/users/:userId/fcm-token', verifyAuth, async (req, res) => {
    const userId = req.params.userId;
    const { fcmToken } = req.body; 

    if (!fcmToken) {
        return res.status(400).send({ message: 'FCM token is required.' });
    }

    try {
        const userRef = db.collection('users').doc(userId);
        
        await userRef.update({
            fcmTokens: admin.firestore.FieldValue.arrayUnion(fcmToken),
        }, { merge: true }); 

        console.log(`[FCM] Token synced for user ${userId}.`);
        res.status(200).send({ message: 'Token registered.' });

    } catch (error) {
        console.error(`[FCM] Error syncing token for ${userId}:`, error);
        res.status(500).send({ message: 'Failed to update FCM token.' });
    }
});


// ENDPOINT B: SEND DAILY WORD PUSH 
router.post('/send-daily-word-push', async (req, res) => {
    const dailyWord = req.body.word || 'STORM'; 

    try {
        // 1. Get ALL users' tokens
        const usersSnapshot = await db.collection('users').get();
        let tokens = [];

        usersSnapshot.forEach(doc => {
            const userData = doc.data();
            if (userData.fcmTokens && Array.isArray(userData.fcmTokens)) {
                tokens = tokens.concat(userData.fcmTokens);
            }
        });
        
        if (tokens.length === 0) {
            return res.status(200).send({ message: 'No tokens found to send push.' });
        }

        // 2. Construct the push message payload
        const message = {
            notification: {
                title: 'WordVenture Daily Challenge!',
                body: `A new daily challenge word is ready to play!`,
            },
            data: {
                type: 'DAILY_WORD',
                wordId: dailyWord,
            },
            tokens: tokens, 
        };

        // 3. Send the message
        const response = await admin.messaging().sendMulticast(message);
        
        console.log(`[FCM] Successfully sent message to ${response.successCount} devices.`);
        
        
        res.status(200).send({ 
            message: 'Push notifications sent.',
            successCount: response.successCount,
            failureCount: response.failureCount
        });

    } catch (error) {
        console.error("[FCM] Error sending daily push:", error);
        res.status(500).send({ message: 'Failed to send notifications.' });
    }
});

module.exports = router;