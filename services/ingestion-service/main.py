import os
import json
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from pymongo import MongoClient
from typing import List, Optional
from datetime import datetime
import aio_pika
import asyncio

app = FastAPI()

# MongoDB connection
MONGO_URL = os.getenv("MONGO_URL", "mongodb://localhost:27017")
RABBITMQ_URL = os.getenv("RABBITMQ_URL", "amqp://guest:guest@localhost/")

mongo_client = MongoClient(MONGO_URL)
db = mongo_client['raag_projects']
projects_collection = db['projects']
requirements_collection = db['requirements']

class Requirement(BaseModel):
    text: str
    type: Optional[str] = None

class Project(BaseModel):
    name: str
    description: str
    requirements: List[Requirement]
    proposed_architecture: str
    use_selected_architecture_for_report: Optional[bool] = True
    domain: Optional[str] = None

async def emit_to_rabbitmq(message: dict):
    try:
        connection = await aio_pika.connect_robust(RABBITMQ_URL)
        async with connection:
            channel = await connection.channel()
            exchange = await channel.declare_exchange('ingestion', aio_pika.ExchangeType.FANOUT, durable=True)
            
            msg = aio_pika.Message(body=json.dumps(message).encode())
            await exchange.publish(msg, routing_key='')
    except Exception as e:
        print(f"RabbitMQ error: {e}")

@app.post("/projects")
async def create_project(project: Project):
    try:
        project_doc = {
            "name": project.name,
            "description": project.description,
            "proposed_architecture": project.proposed_architecture,
            "use_selected_architecture_for_report": project.use_selected_architecture_for_report,
            "domain": project.domain,
            "created_at": datetime.utcnow(),
            "status": "ingested"
        }
        
        result = projects_collection.insert_one(project_doc)
        project_id = str(result.inserted_id)
        
        # Store requirements
        for req in project.requirements:
            req_doc = {
                "project_id": project_id,
                "text": req.text,
                "classification": None,
                "created_at": datetime.utcnow()
            }
            requirements_collection.insert_one(req_doc)
        
        # Emit to RabbitMQ for LLM analysis
        await emit_to_rabbitmq({
            "project_id": project_id,
            "action": "analyze",
            "timestamp": datetime.utcnow().isoformat()
        })
        
        return {"project_id": project_id, "status": "created"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/projects/{project_id}")
async def get_project(project_id: str):
    try:
        from bson import ObjectId
        project = projects_collection.find_one({"_id": ObjectId(project_id)})
        requirements = list(requirements_collection.find({"project_id": project_id}))
        
        if not project:
            raise HTTPException(status_code=404, detail="Project not found")
        
        project['_id'] = str(project['_id'])
        for req in requirements:
            req['_id'] = str(req['_id'])
        
        return {"project": project, "requirements": requirements}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/projects/{project_id}/requirements")
async def add_requirement(project_id: str, requirement: Requirement):
    try:
        req_doc = {
            "project_id": project_id,
            "text": requirement.text,
            "classification": None,
            "created_at": datetime.utcnow()
        }
        result = requirements_collection.insert_one(req_doc)
        return {"requirement_id": str(result.inserted_id)}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/health")
async def health():
    return {"status": "ok", "service": "ingestion"}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=int(os.getenv("PORT", 8001)))
