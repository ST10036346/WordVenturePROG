const express = require("express");
const fs = require("fs");
const path = require("path");

const app = express();
const PORT = process.env.PORT || 3000; // ✅ use Render's port

app.get("/random-word", (req, res) => {
  const words = fs.readFileSync(path.join(__dirname, "words.txt"), "utf-8")
    .split("\n")
    .filter(Boolean);
  const randomWord = words[Math.floor(Math.random() * words.length)];
  res.json({ word: randomWord });
});

app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
