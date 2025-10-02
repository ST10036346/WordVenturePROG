const express = require("express");
const fs = require("fs");
const path = require("path");

const app = express();
const PORT = 3000;

// Read words from the file
const wordsFile = path.join(__dirname, "words.txt");
let words = [];

// Load words into memory
fs.readFile(wordsFile, "utf8", (err, data) => {
  if (err) {
    console.error("Error reading words file:", err);
  } else {
    words = data.split(/\r?\n/).filter((word) => word.trim().length > 0);
    console.log(`Loaded ${words.length} words.`);
  }
});

// API endpoint: return a random word
app.get("/random-word", (req, res) => {
  if (words.length === 0) {
    return res.status(500).json({ error: "No words loaded." });
  }
  const randomWord = words[Math.floor(Math.random() * words.length)];
  res.json({ word: randomWord });
});

// Start server
app.listen(PORT, () => {
  console.log(`Word API running at http://localhost:${PORT}`);
});