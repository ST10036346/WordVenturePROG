const express = require("express");
const fs = require("fs");
const app = express();
const PORT = process.env.PORT || 3000; // use environment port when hosted

// Load words from JSON file
const words = JSON.parse(fs.readFileSync("words.json", "utf8"));

// Endpoint: random word
app.get("/random-word", (req, res) => {
    const word = words[Math.floor(Math.random() * words.length)];
    res.json({ word });
});

// Root endpoint
app.get("/", (req, res) => {
    res.send("Word API is running!");
});

app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
