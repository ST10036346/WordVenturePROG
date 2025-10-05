const express = require("express");
const cors = require("cors");
const fs = require("fs");
const path = require("path");

const app = express();
const PORT = process.env.PORT || 3000; // ✅ use Render's assigned port

// ✅ Enable CORS (so Android Studio or Postman can access the API)
app.use(cors());
app.use(express.json());

// ✅ Load words once on startup
const words = fs.readFileSync(path.join(__dirname, "words.txt"), "utf-8")
  .split("\n")
  .map(w => w.trim().toLowerCase())
  .filter(Boolean);

// ✅ Endpoint: get a random word
app.get("/random-word", (req, res) => {
  const randomWord = words[Math.floor(Math.random() * words.length)];
  res.json({ word: randomWord });
});

// ✅ Endpoint: check if a word is valid
app.post("/check-word", (req, res) => {
  const { guess } = req.body;

  if (!guess) {
    return res.status(400).json({ valid: false, message: "No word provided" });
  }

  const isValid = words.includes(guess.toLowerCase());
  res.json({ valid: isValid });
});

// ✅ Simple home route for testing
app.get("/", (req, res) => {
  res.send("✅ Word API is running and ready!");
});

app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
