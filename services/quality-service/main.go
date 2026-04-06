package main

import (
	"database/sql"
	"log"
	"net/http"
	"os"
	"fmt"

	"github.com/gin-gonic/gin"
	_ "github.com/lib/pq"
)

var db *sql.DB

type QualityScore struct {
	RequirementID string  `json:"requirement_id"`
	Text          string  `json:"text"`
	Score         int     `json:"score"`
	IsVague       bool    `json:"is_vague"`
	Completeness  float64 `json:"completeness"`
}

func init() {
	dbURL := fmt.Sprintf("postgres://%s:%s@%s/%s?sslmode=disable",
		os.Getenv("DB_USER"),
		os.Getenv("DB_PASSWORD"),
		os.Getenv("DB_HOST"),
		os.Getenv("DB_NAME"),
	)

	var err error
	db, err = sql.Open("postgres", dbURL)
	if err != nil {
		log.Fatal(err)
	}

	err = db.Ping()
	if err != nil {
		log.Fatal(err)
	}

	createTables()
	log.Println("✓ PostgreSQL connected")
}

func createTables() {
	query := `
	CREATE TABLE IF NOT EXISTS quality_scores (
		id SERIAL PRIMARY KEY,
		requirement_id VARCHAR(255),
		text TEXT,
		score INT,
		is_vague BOOLEAN,
		completeness FLOAT,
		created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
	);
	`
	db.Exec(query)
}

func main() {
	router := gin.Default()

	router.POST("/quality-check", qualityCheck)
	router.GET("/quality/:requirement_id", getQuality)
	router.GET("/health", health)

	port := os.Getenv("PORT")
	if port == "" {
		port = "8004"
	}

	log.Printf("🚀 Quality Service running on port %s", port)
	router.Run(":" + port)
}

func qualityCheck(c *gin.Context) {
	var req struct {
		RequirementID string `json:"requirement_id"`
		Text          string `json:"text"`
	}

	if err := c.BindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	// IEEE 830 quality scoring
	score := 100
	isVague := false
	vagueWords := []string{"fast", "easy", "good", "sometimes", "user-friendly"}

	for _, word := range vagueWords {
		if contains(req.Text, word) {
			isVague = true
			score -= 20
		}
	}

	completeness := float64(score) / 100.0

	query := `INSERT INTO quality_scores (requirement_id, text, score, is_vague, completeness) 
	VALUES ($1, $2, $3, $4, $5) RETURNING id`

	var id int
	err := db.QueryRow(query, req.RequirementID, req.Text, score, isVague, completeness).Scan(&id)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}

	c.JSON(http.StatusOK, QualityScore{
		RequirementID: req.RequirementID,
		Text:          req.Text,
		Score:         score,
		IsVague:       isVague,
		Completeness:  completeness,
	})
}

func getQuality(c *gin.Context) {
	requirementID := c.Param("requirement_id")

	var quality QualityScore
	query := `SELECT requirement_id, text, score, is_vague, completeness FROM quality_scores 
	WHERE requirement_id = $1 LIMIT 1`

	err := db.QueryRow(query, requirementID).Scan(
		&quality.RequirementID,
		&quality.Text,
		&quality.Score,
		&quality.IsVague,
		&quality.Completeness,
	)

	if err != nil {
		c.JSON(http.StatusNotFound, gin.H{"error": "Quality score not found"})
		return
	}

	c.JSON(http.StatusOK, quality)
}

func health(c *gin.Context) {
	c.JSON(http.StatusOK, gin.H{"status": "ok", "service": "quality"})
}

func contains(s, substr string) bool {
	for i := 0; i <= len(s)-len(substr); i++ {
		if s[i:i+len(substr)] == substr {
			return true
		}
	}
	return false
}
