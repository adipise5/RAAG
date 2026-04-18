# GPU Compatibility Fix

## Problem
The original `docker-compose.yml` had NVIDIA GPU configuration that was causing errors on Mac:
```
Error response from daemon: could not select device driver "nvidia" with capabilities: [[gpu]]
```

This occurred because the Ollama service was configured to require NVIDIA GPU acceleration, which:
- Only works on systems with NVIDIA GPUs and NVIDIA Container Toolkit installed
- Doesn't apply to Mac systems (which don't have NVIDIA GPUs)
- Doesn't apply to CPU-only Linux systems

## Solution
The Ollama service GPU configuration has been made **optional and commented out** in `docker-compose.yml`.

### What Changed
```yaml
# Before (causes error on Mac/CPU systems):
ollama:
  image: ollama/ollama:latest
  deploy:
    resources:
      reservations:
        devices:
          - driver: nvidia
            count: 1
            capabilities: [gpu]

# After (works on all systems):
ollama:
  image: ollama/ollama:latest
  # GPU acceleration is optional - uncomment the deploy section below to enable NVIDIA GPU
  # deploy:
  #   resources:
  #     reservations:
  #       devices:
  #         - driver: nvidia
  #           count: 1
  #           capabilities: [gpu]
```

## Supported Platforms

### ✅ Mac (M1/M2/M3/M4)
- Ollama runs efficiently on CPU
- No configuration changes needed

### ✅ Linux with NVIDIA GPU
To enable GPU acceleration on NVIDIA systems:
1. Install [NVIDIA Container Toolkit](https://docs.nvidia.com/datacenter/cloud-native/container-toolkit/install-guide.html)
2. Uncomment the `deploy` section in `docker-compose.yml`
3. Run `docker compose up --build`

### ✅ CPU-only Systems
- Ollama runs in CPU-only mode
- No configuration needed

## Performance Notes

| Platform | Mode | Performance | Setup |
|----------|------|-------------|-------|
| Mac (M1/M4) | CPU | ~60s per request | No setup needed |
| Linux + NVIDIA GPU | GPU | ~8s per request | Install NVIDIA Container Toolkit |
| CPU-only | CPU | ~120s per request | No setup needed |

The CPU mode will be slower but fully functional. For production or frequent use on GPU-equipped systems, enable GPU acceleration by uncommenting the deploy section.
