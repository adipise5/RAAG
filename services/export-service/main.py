import os
import json
from fastapi import FastAPI, HTTPException
from fastapi.responses import StreamingResponse
from pydantic import BaseModel
from pymongo import MongoClient
from datetime import datetime
from typing import Optional, List
from io import BytesIO
import base64

app = FastAPI()

MONGO_URL = os.getenv("MONGO_URL", "mongodb://localhost:27017")

mongo_client = MongoClient(MONGO_URL)
db = mongo_client['raag_projects']
projects_collection = db['projects']
requirements_collection = db['requirements']
analysis_collection = db['analysis']

class ExportRequest(BaseModel):
    project_id: str
    include_diagrams: bool = True
    include_analysis: bool = True

def generate_html_report(project_id: str, include_diagrams: bool = True) -> str:
    """Generate HTML report from project data"""
    try:
        from bson import ObjectId
        project = projects_collection.find_one({"_id": ObjectId(project_id)})
        requirements = list(requirements_collection.find({"project_id": project_id}))
        analysis = analysis_collection.find_one({"project_id": project_id}, sort=[("analyzed_at", -1)])
        
        html = f"""
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <style>
                body {{ font-family: Arial, sans-serif; margin: 40px; color: #333; }}
                .header {{ text-align: center; border-bottom: 3px solid #2c3e50; padding-bottom: 20px; }}
                h1 {{ color: #2c3e50; }}
                h2 {{ color: #34495e; margin-top: 30px; border-left: 4px solid #3498db; padding-left: 10px; }}
                .metric {{ background: #ecf0f1; padding: 10px; margin: 10px 0; border-radius: 5px; }}
                table {{ width: 100%; border-collapse: collapse; margin: 20px 0; }}
                th, td {{ border: 1px solid #bdc3c7; padding: 12px; text-align: left; }}
                th {{ background: #3498db; color: white; }}
                tr:nth-child(even) {{ background: #f9f9f9; }}
                .classification-fr {{ background: #d5f4e6; color: #27ae60; padding: 3px 8px; border-radius: 3px; }}
                .classification-nfr {{ background: #fadbd8; color: #e74c3c; padding: 3px 8px; border-radius: 3px; }}
                footer {{ margin-top: 40px; text-align: center; color: #7f8c8d; font-size: 12px; }}
            </style>
        </head>
        <body>
            <div class="header">
                <h1>RAAG - Requirements Analysis & Architecture Generator</h1>
                <h2>{project.get('name', 'Untitled Project')}</h2>
                <p><strong>Generated:</strong> {datetime.utcnow().strftime('%Y-%m-%d %H:%M:%S')} UTC</p>
            </div>

            <h2>Project Overview</h2>
            <div class="metric">
                <p><strong>Description:</strong> {project.get('description', 'N/A')}</p>
                <p><strong>Proposed Architecture:</strong> {project.get('proposed_architecture', 'N/A')}</p>
                <p><strong>Domain:</strong> {project.get('domain', 'General')}</p>
            </div>

            <h2>Requirements Summary</h2>
            <div class="metric">
                <p><strong>Total Requirements:</strong> {len(requirements)}</p>
            </div>

            <h2>Requirements List</h2>
            <table>
                <thead>
                    <tr>
                        <th>ID</th>
                        <th>Requirement Text</th>
                        <th>Classification</th>
                    </tr>
                </thead>
                <tbody>
        """
        
        for idx, req in enumerate(requirements, 1):
            classification = req.get('classification', 'Pending')
            class_badge = f'<span class="classification-fr">{classification}</span>' if classification == 'FR' else f'<span class="classification-nfr">{classification}</span>'
            html += f"""
                    <tr>
                        <td>{idx}</td>
                        <td>{req.get('text', '')}</td>
                        <td>{class_badge}</td>
                    </tr>
            """
        
        html += """
                </tbody>
            </table>
        """
        
        if analysis:
            html += f"""
            <h2>Quality Analysis</h2>
            <div class="metric">
                <p><strong>Overall Quality Score:</strong> {analysis.get('overall_quality', 0)}/100</p>
                <p><strong>Analysis Date:</strong> {analysis.get('analyzed_at', 'N/A')}</p>
            </div>
            
            <h2>Requirement Classifications</h2>
            <table>
                <thead>
                    <tr>
                        <th>Index</th>
                        <th>Classification</th>
                        <th>Confidence</th>
                        <th>Quality Score</th>
                        <th>Vague</th>
                    </tr>
                </thead>
                <tbody>
            """
            
            for cls in analysis.get('classifications', []):
                html += f"""
                    <tr>
                        <td>{cls.get('requirement_index')}</td>
                        <td>{cls.get('classification')}</td>
                        <td>{cls.get('confidence', 0):.0%}</td>
                        <td>{cls.get('quality_score', 0)}</td>
                        <td>{'Yes' if cls.get('is_vague') else 'No'}</td>
                    </tr>
                """
            
            html += """
                </tbody>
            </table>
            """
        
        html += f"""
            <footer>
                <p>RAAG v2.0 | Project ID: {project_id}</p>
                <p>This report was auto-generated. For detailed analysis, visit the RAAG dashboard.</p>
            </footer>
        </body>
        </html>
        """
        
        return html
    except Exception as e:
        return f"<html><body><h1>Error generating report</h1><p>{str(e)}</p></body></html>"

@app.post("/export/{project_id}")
async def export_to_pdf(project_id: str, request: Optional[ExportRequest] = None):
    """Export project to PDF"""
    try:
        html_content = generate_html_report(project_id)
        
        # For now, return as HTML. In production, use WeasyPrint to convert to PDF
        from weasyprint import HTML, CSS
        from io import BytesIO
        
        # Create PDF from HTML
        pdf_bytes = HTML(string=html_content).write_pdf()
        
        return StreamingResponse(
            iter([pdf_bytes]),
            media_type="application/pdf",
            headers={
                "Content-Disposition": f"attachment; filename=raag-report-{project_id}.pdf"
            }
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/export/{project_id}")
async def get_export_preview(project_id: str):
    """Get HTML preview of the report"""
    try:
        html = generate_html_report(project_id)
        from fastapi.responses import HTMLResponse
        return HTMLResponse(content=html)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/health")
async def health():
    return {"status": "ok", "service": "export"}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=int(os.getenv("PORT", 8006)))
