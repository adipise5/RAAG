FROM golang:1.21-alpine AS builder

WORKDIR /app
# Cache dependencies before copying source
COPY go.mod ./
RUN go mod download

COPY . .
RUN go mod tidy && CGO_ENABLED=0 GOOS=linux go build -o user-service .

FROM alpine:3.19
RUN apk --no-cache add ca-certificates
WORKDIR /root/
COPY --from=builder /app/user-service .

EXPOSE 8008
CMD ["./user-service"]
