FROM bitnami/node:10.19.0

# Copy the application code
COPY . /app

# Set working directory
WORKDIR /app

# Create a non-root user
RUN useradd -r -u 1001 -g root nonroot
RUN chown -R nonroot /app
USER nonroot

# Set the application port to 3000
ENV PORT="3000"
ENV SOURCE_PASSWORD="CHANGE_ME_FOR_SOURCE_PASSWORD"
ENV COMPOSED_PASSWORD="CHANGE_ME_FOR_COMPOSED_PASSWORD"

# Start the application
CMD ["node", "./bin/www"]