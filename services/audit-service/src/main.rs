use actix_web::{web, App, HttpServer, HttpResponse, middleware};
use serde::{Deserialize, Serialize};
use sqlx::postgres::PgPool;
use sqlx::Row;
use chrono::{DateTime, Utc};
use std::env;
use std::time::Duration;
use tokio::time::sleep;

#[derive(Debug, Serialize, Deserialize, Clone)]
struct AuditEvent {
    event_id: String,
    timestamp: DateTime<Utc>,
    service_name: String,
    endpoint: String,
    http_status: i32,
    latency_ms: i64,
    ip_address: String,
}

#[derive(Debug, Serialize, Deserialize)]
struct AuditStats {
    total_events: i64,
    events_today: i64,
    error_rate: f64,
    avg_latency_ms: f64,
}

async fn health() -> HttpResponse {
    HttpResponse::Ok().json(serde_json::json!({
        "status": "ok",
        "service": "audit"
    }))
}

async fn get_stats(pool: web::Data<PgPool>) -> HttpResponse {
    match sqlx::query_scalar::<_, i64>(
        "SELECT COUNT(*) FROM audit_logs"
    )
    .fetch_one(pool.as_ref())
    .await {
        Ok(count) => {
            let avg_latency = sqlx::query_scalar::<_, Option<f64>>(
                "SELECT AVG(latency_ms) FROM audit_logs WHERE timestamp > NOW() - INTERVAL '24 hours'"
            )
            .fetch_one(pool.as_ref())
            .await
            .unwrap_or(Some(0.0))
            .unwrap_or(0.0);

            HttpResponse::Ok().json(AuditStats {
                total_events: count,
                events_today: count / 10, // Mock value
                error_rate: 0.02,
                avg_latency_ms: avg_latency,
            })
        }
        Err(_) => HttpResponse::InternalServerError().finish()
    }
}

async fn get_logs(pool: web::Data<PgPool>) -> HttpResponse {
    match sqlx::query(
        "SELECT event_id, timestamp, service_name, endpoint, http_status, latency_ms, ip_address 
         FROM audit_logs ORDER BY timestamp DESC LIMIT 100"
    )
    .fetch_all(pool.as_ref())
    .await {
        Ok(rows) => {
            let logs: Vec<AuditEvent> = rows.iter().map(|row| {
                AuditEvent {
                    event_id: row.get(0),
                    timestamp: row.get(1),
                    service_name: row.get(2),
                    endpoint: row.get(3),
                    http_status: row.get(4),
                    latency_ms: row.get(5),
                    ip_address: row.get(6),
                }
            }).collect();
            HttpResponse::Ok().json(logs)
        }
        Err(_) => HttpResponse::InternalServerError().finish()
    }
}

async fn init_db(pool: &PgPool) {
    sqlx::query(
        r#"
        CREATE TABLE IF NOT EXISTS audit_logs (
            id SERIAL PRIMARY KEY,
            event_id VARCHAR(36),
            timestamp TIMESTAMP WITH TIME ZONE,
            service_name VARCHAR(255),
            endpoint VARCHAR(255),
            http_status INTEGER,
            latency_ms BIGINT,
            ip_address VARCHAR(45),
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
        "#
    )
    .execute(pool)
    .await
    .expect("Failed to create tables");

    sqlx::query("CREATE INDEX IF NOT EXISTS idx_audit_timestamp ON audit_logs(timestamp)")
        .execute(pool)
        .await
        .expect("Failed to create idx_audit_timestamp");

    sqlx::query("CREATE INDEX IF NOT EXISTS idx_audit_service ON audit_logs(service_name)")
        .execute(pool)
        .await
        .expect("Failed to create idx_audit_service");
}

#[actix_web::main]
async fn main() -> std::io::Result<()> {
    env_logger::init_from_env(env_logger::Env::new().default_filter_or("info"));

    let db_url = env::var("DATABASE_URL")
        .unwrap_or_else(|_| {
            format!(
                "postgres://{}:{}@{}:5432/{}",
                env::var("DB_USER").unwrap_or_else(|_| "admin".to_string()),
                env::var("DB_PASSWORD").unwrap_or_else(|_| "password".to_string()),
                env::var("DB_HOST").unwrap_or_else(|_| "localhost".to_string()),
                env::var("DB_NAME").unwrap_or_else(|_| "raag".to_string()),
            )
        });

    // Try connecting to Postgres with retries in case the DB container isn't ready yet.
    let pool = loop {
        match PgPool::connect(&db_url).await {
            Ok(p) => break p,
            Err(e) => {
                eprintln!("Failed to connect to database: {}. Retrying in 3s...", e);
                sleep(Duration::from_secs(3)).await;
            }
        }
    };
    init_db(&pool).await;

    let pool = web::Data::new(pool);
    let port = env::var("PORT").unwrap_or_else(|_| "8005".to_string());
    let bind_addr = format!("0.0.0.0:{}", port);

    println!("🚀 Audit Service running on {}", bind_addr);

    HttpServer::new(move || {
        App::new()
            .app_data(pool.clone())
            .wrap(middleware::Logger::default())
            .route("/health", web::get().to(health))
            .route("/audit/stats", web::get().to(get_stats))
            .route("/audit/logs", web::get().to(get_logs))
    })
    .bind(&bind_addr)?
    .run()
    .await
}
