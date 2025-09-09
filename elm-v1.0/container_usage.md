## Usage Instructions

### Frontend

#### Development Mode:

bash

```bash
# Navigate to frontend directory
cd ./elmboot/

# Start development container with hot reload
docker-compose up

# Access at: http://localhost:8081
```

#### Production Mode:

bash

```bash
# Build and start production container
docker-compose -f docker-compose.yml up -d --build

# Access at: http://localhost
```

### Backend

#### Development Mode:

bash

```bash
# Navigate to backend directory
cd ./elmclient/

# Start development container
docker-compose up

# Access at: http://localhost:8080
```

#### Production Mode:

bash

```bash
# Build and start production container
docker-compose -f docker-compose.yml up -d --build

# Access at: http://localhost:8080
```

#### Clean up and rebuild:

bash

```bash
docker-compose down
docker system prune -f
docker-compose up --build
```

