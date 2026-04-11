FROM rust:1.88-slim AS builder

WORKDIR /app

# Cache dependencies separately from source
COPY Cargo.toml ./
# Create a dummy main so cargo can resolve and compile all deps
RUN mkdir src && echo "fn main() {}" > src/main.rs
RUN cargo build --release
# Remove the dummy artifact so the real build links correctly
RUN rm -f target/release/audit-service target/release/deps/audit_service*

# Now copy the real source and rebuild (only app code recompiles)
COPY src ./src
RUN cargo build --release

FROM debian:bookworm-slim

RUN apt-get update && apt-get install -y libssl3 ca-certificates && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=builder /app/target/release/audit-service .

EXPOSE 8005

CMD ["./audit-service"]
