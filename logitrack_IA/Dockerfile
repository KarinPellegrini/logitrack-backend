FROM python:3.11-slim
WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY RandomForestIA.py .
EXPOSE 5001
CMD ["sh", "-c", "gunicorn --bind 0.0.0.0:${PORT:-5001} --timeout 120 RandomForestIA:app"]