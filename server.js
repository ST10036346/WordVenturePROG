const express = require("express");
const fs = require("fs");
const path = require("path");

const app = express();
const PORT = process.env.PORT || 3000; // ✅ use Render's port

app.use(express.json());

app.get("/random-word", (req, res) => {
  const words = fs.readFileSync(path.join(__dirname, "words.txt"), "utf-8")
    .split("\n")
    .filter(Boolean);
  const randomWord = words[Math.floor(Math.random() * words.length)];
  res.json({ word: randomWord });
});
app.post("/check-word", (req, res) => {
  const { guess } = req.body;

  if (!guess) {
    return res.status(400).json({ valid: false, message: "No word provided" });
  }

  const isValid = words.includes(guess.toLowerCase() );
  res.json({ valid: isValid });
});
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
