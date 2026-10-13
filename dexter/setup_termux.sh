#!/bin/bash
echo "Starting dexter setup for Termux..."

# Update and upgrade termux packages
pkg update -y && pkg upgrade -y

# Install essential build tools and dependencies
pkg install -y python clang make cmake libopenblas binutils rust git

# Upgrade pip
python -m pip install --upgrade pip

# Install numpy (pre-compiled usually better on termux, but we build if needed)
MATHLIB="m" pip install numpy

# Install llama-cpp-python with OpenBLAS for CPU acceleration
echo "Installing llama-cpp-python (this may take a while to compile)..."
CMAKE_ARGS="-DGGML_BLAS=ON -DGGML_BLAS_VENDOR=OpenBLAS" pip install llama-cpp-python

# Install other dependencies
pip install sqlite3-api transformers torch sentencepiece

# Create necessary directories for models and database
mkdir -p models
mkdir -p data

echo "Setup complete! Please download your models into the 'models' directory."
echo "1. Download Qwen2.5-0.5B-Instruct GGUF into models/"
echo "2. Download laya into models/laya/"
