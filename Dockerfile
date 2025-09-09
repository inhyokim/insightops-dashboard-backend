# Backend Dockerfile for insightops-dashboard-backend
FROM node:18-alpine

WORKDIR /app

# Copy package files first (for better caching)
COPY package*.json ./

# Install dependencies
RUN npm install

# Copy source code
COPY . .

# Expose port (adjust as needed for your backend)
EXPOSE 3000

# Start the backend application
CMD ["npm", "start"]