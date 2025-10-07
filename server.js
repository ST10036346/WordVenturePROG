// server.js
// Author: Ethan Pillay
// Description: Simple Word API for Wordle-style game
// Source/Attribution: Custom code, adapted concepts from Express and Node.js documentation

const express = require("express");
const cors = require("cors");
const fs = require("fs");
const path = require("path");

const app = express();
const PORT = process.env.PORT || 3000; // Use Render's assigned port

// Enable CORS so external apps (like Android Studio or Postman) can access the API
app.use(cors());
app.use(express.json());

// Load words once at startup
// Source for words: public domain word list (words.txt)
const words = fs.readFileSync(path.join(__dirname, "words.txt"), "utf-8")
  .split("\n")
  .map(w => w.trim().toLowerCase())
  .filter(Boolean);

// GET /random-word
// Returns a random word from the list
app.get("/random-word", (req, res) => {
  const randomWord = words[Math.floor(Math.random() * words.length)];
  res.json({ word: randomWord });
});

// POST /check-word
// Checks if the provided word exists in the list
app.post("/check-word", (req, res) => {
  const { guess } = req.body;

  if (!guess) {
    return res.status(400).json({ valid: false, message: "No word provided" });
  }

  const isValid = words.includes(guess.toLowerCase());
  res.json({ valid: isValid });
});

// GET /
// Simple home route for testing
app.get("/", (req, res) => {
  res.send(" Word API is running and ready!");
});

// Start the server
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
