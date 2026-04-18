FROM node:18-alpine AS builder

WORKDIR /app

COPY package.json .
RUN npm install

COPY . .
RUN npm run build

FROM nginx:1.27-alpine

# Copy nginx config for port 3000 and React routing
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy built React app
COPY --from=builder /app/build /usr/share/nginx/html

# Remove default nginx config
RUN rm -f /etc/nginx/conf.d/default.conf.orig

EXPOSE 3000

CMD ["nginx", "-g", "daemon off;"]
