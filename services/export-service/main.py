import os
import json
import asyncio
from fastapi import FastAPI, HTTPException
from fastapi.responses import StreamingResponse, Response
from pydantic import BaseModel
from pymongo import MongoClient
from datetime import datetime
from typing import Optional, List
from io import BytesIO
import base64
from weasyprint import HTML

app = FastAPI()

MONGO_URL = os.getenv("MONGO_URL", "mongodb://localhost:27017")

mongo_client = MongoClient(MONGO_URL)
db = mongo_client['raag_projects']
db_arch = mongo_client['raag_architecture']
projects_collection = db['projects']
requirements_collection = db['requirements']
analysis_collection = db['analysis']
architecture_reports_collection = db_arch['architecture_reports']

class ExportRequest(BaseModel):
    project_id: str
    include_diagrams: bool = True
    include_analysis: bool = True

def escape_html(text):
    """Escape HTML special characters"""
    if not text:
        return ''
    return str(text).replace('&', '&amp;').replace('<', '&lt;').replace('>', '&gt;').replace('"', '&quot;').replace("'", '&#39;')

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
                <p><strong>Ambiguous Requirements:</strong> {analysis.get('quality_summary', {}).get('ambiguous_count', 0)}</p>
                <p><strong>High-Risk Requirements:</strong> {analysis.get('quality_summary', {}).get('high_risk', 0)}</p>
                <p><strong>Analysis Date:</strong> {analysis.get('analyzed_at', 'N/A')}</p>
            </div>
            
            <h2>Requirement Classifications</h2>
            <table>
                <thead>
                    <tr>
                        <th>#</th>
                        <th>Requirement</th>
                        <th>Type</th>
                        <th>Priority</th>
                        <th>Confidence</th>
                        <th>Quality</th>
                        <th>Vague</th>
                    </tr>
                </thead>
                <tbody>
            """
            
            for cls in analysis.get('classifications', []):
                priority = cls.get('priority', 'Medium')
                priority_color = '#e74c3c' if priority == 'High' else '#f39c12' if priority == 'Medium' else '#27ae60'
                html += f"""
                    <tr>
                        <td>{cls.get('requirement_index', 0) + 1}</td>
                        <td>{cls.get('text', '')}</td>
                        <td><span class="classification-{'fr' if cls.get('classification') == 'FR' else 'nfr'}">{cls.get('classification')}</span></td>
                        <td style="color: {priority_color}; font-weight: bold;">{priority}</td>
                        <td>{cls.get('confidence', 0):.0%}</td>
                        <td>{cls.get('quality_score', 0)}%</td>
                        <td>{'Yes' if cls.get('is_vague') else 'No'}</td>
                    </tr>
                """
            
            html += """
                </tbody>
            </table>
            """

            # SMART Rewrites section
            vague_or_low = [c for c in analysis.get('classifications', []) if c.get('is_vague') or c.get('quality_score', 100) < 70]
            if vague_or_low:
                html += """<h2>Suggested SMART Rewrites</h2>"""
                for cls in vague_or_low:
                    missing = ', '.join(cls.get('missing_elements', []))
                    html += f"""
                    <div class="metric" style="border-left: 4px solid #f39c12;">
                        <p><strong>Requirement #{cls.get('requirement_index', 0) + 1}</strong> (Quality: {cls.get('quality_score', 0)}%{f' — Missing: {missing}' if missing else ''})</p>
                        <p><strong>Original:</strong> {cls.get('text', '')}</p>
                        <p style="color: #27ae60;"><strong>Rewrite:</strong> {cls.get('rewritten_requirement', '') or cls.get('improved_requirement', '')}</p>
                    </div>
                    """

            # Extracted Elements section
            all_entities = list(set(e for c in analysis.get('classifications', []) for e in c.get('entities', [])))
            all_processes = list(set(p for c in analysis.get('classifications', []) for p in c.get('processes', [])))
            all_data_stores = list(set(d for c in analysis.get('classifications', []) for d in c.get('data_stores', [])))
            if all_entities or all_processes or all_data_stores:
                html += """<h2>Extracted Elements</h2><div class="metric">"""
                if all_entities:
                    html += f"""<p><strong>Entities:</strong> {', '.join(all_entities)}</p>"""
                if all_processes:
                    html += f"""<p><strong>Processes:</strong> {', '.join(all_processes)}</p>"""
                if all_data_stores:
                    html += f"""<p><strong>Data Stores:</strong> {', '.join(all_data_stores)}</p>"""
                html += """</div>"""

        # ======== Architecture Data ========
        arch_report = architecture_reports_collection.find_one(
            {"projectId": project_id},
            sort=[("savedAt", -1)]
        )
        if arch_report:
            recommended = arch_report.get('recommendedStyle', 'N/A')
            proposed = arch_report.get('proposedStyle', 'N/A')
            complexity = arch_report.get('complexity', 0)

            html += f"""
            <h2>Architecture Recommendation</h2>
            <div class="metric">
                <p><strong>Recommended Style:</strong> {escape_html(recommended)}</p>
                <p><strong>Proposed Style:</strong> {escape_html(proposed)}</p>
                <p><strong>Complexity Score:</strong> {complexity}</p>
            </div>
            """

            # Justification
            justification = arch_report.get('justification', [])
            if justification:
                html += """<h3 style="margin-top: 20px; color: #34495e;">Justification</h3><ul>"""
                for item in justification:
                    html += f"<li>{escape_html(item)}</li>"
                html += "</ul>"

            # Comparison
            comparison = arch_report.get('comparison', [])
            if comparison and comparison != ['Proposed architecture matches recommendation.']:
                html += f"""<h3 style="margin-top: 20px; color: #34495e;">Why {escape_html(recommended)} over {escape_html(proposed)}?</h3><ul>"""
                for item in comparison:
                    html += f"<li>{escape_html(item)}</li>"
                html += "</ul>"

            # DFD Diagrams
            dfd = arch_report.get('dfd', {})
            if include_diagrams and dfd:
                level_0 = dfd.get('level_0', {})
                level_1 = dfd.get('level_1', {})
                if level_0.get('svg') or level_1.get('svg'):
                    html += """<h2>Data Flow Diagrams</h2>"""
                    if level_0.get('svg'):
                        html += f"""
                        <h3 style="color: #34495e;">Level 0 — System Context</h3>
                        <div style="border: 1px solid #bdc3c7; border-radius: 5px; padding: 15px; margin: 15px 0; background: #fff; overflow: auto;">
                            {level_0['svg']}
                        </div>
                        """
                    if level_1.get('svg'):
                        html += f"""
                        <h3 style="color: #34495e;">Level 1 — Component Flow</h3>
                        <div style="border: 1px solid #bdc3c7; border-radius: 5px; padding: 15px; margin: 15px 0; background: #fff; overflow: auto;">
                            {level_1['svg']}
                        </div>
                        """

            # Additional Diagrams (Component, Use Case, Deployment, Sequence)
            additional_diagrams = arch_report.get('additionalDiagrams', [])
            if include_diagrams and additional_diagrams:
                html += """<h2>Architecture Diagrams</h2>"""
                for diagram in additional_diagrams:
                    title = escape_html(diagram.get('title', 'Diagram'))
                    svg = diagram.get('svg', '')
                    if svg:
                        html += f"""
                        <h3 style="color: #34495e;">{title}</h3>
                        <div style="border: 1px solid #bdc3c7; border-radius: 5px; padding: 15px; margin: 15px 0; background: #fff; overflow: auto;">
                            {svg}
                        </div>
                        """

            # Traceability Matrix
            trace = arch_report.get('traceabilityMatrix', {})
            trace_matrix = trace.get('matrix', [])
            if trace_matrix:
                html += """
                <h2>Traceability Matrix</h2>
                <table>
                    <thead>
                        <tr>
                            <th>Requirement</th>
                            <th>Mapped Components</th>
                        </tr>
                    </thead>
                    <tbody>
                """
                for row in trace_matrix:
                    req_text = escape_html(row.get('requirement', ''))
                    comps = row.get('components', [])
                    comp_html = ', '.join(escape_html(c) for c in comps) if comps else '<span style="color: #e74c3c; font-weight: bold;">Untraced</span>'
                    html += f"""
                        <tr>
                            <td>{req_text}</td>
                            <td>{comp_html}</td>
                        </tr>
                    """
                html += "</tbody></table>"

                untraced = trace.get('untraced_requirements', [])
                if untraced:
                    html += f"""<div class="metric" style="border-left: 4px solid #e74c3c;"><p><strong>{len(untraced)} untraced requirement(s)</strong></p></div>"""

            # Gap Analysis
            gaps = arch_report.get('gapAnalysis', [])
            if gaps:
                html += """
                <h2>Gap Analysis</h2>
                <table>
                    <thead><tr><th>Gap</th><th>Severity</th><th>Suggestion</th></tr></thead>
                    <tbody>
                """
                for gap in gaps:
                    severity = gap.get('severity', 'Medium')
                    sev_color = '#e74c3c' if severity == 'Critical' else '#f39c12' if severity == 'High' else '#7f8c8d'
                    html += f"""
                        <tr>
                            <td>{escape_html(gap.get('gap', ''))}</td>
                            <td style="color: {sev_color}; font-weight: bold;">{escape_html(severity)}</td>
                            <td>{escape_html(gap.get('suggestion', ''))}</td>
                        </tr>
                    """
                html += "</tbody></table>"

            # Risk & Assumptions
            risks = arch_report.get('riskAndAssumptions', [])
            if risks:
                html += """
                <h2>Risks &amp; Assumptions</h2>
                <table>
                    <thead><tr><th>Type</th><th>Description</th><th>Severity</th><th>Mitigation</th></tr></thead>
                    <tbody>
                """
                for item in risks:
                    item_type = item.get('type', 'Risk')
                    severity = item.get('severity', 'Medium')
                    type_color = '#e74c3c' if item_type == 'Risk' else '#3498db'
                    sev_color = '#e74c3c' if severity == 'Critical' else '#f39c12' if severity == 'High' else '#7f8c8d'
                    html += f"""
                        <tr>
                            <td style="color: {type_color}; font-weight: bold;">{escape_html(item_type)}</td>
                            <td>{escape_html(item.get('description', ''))}</td>
                            <td style="color: {sev_color}; font-weight: bold;">{escape_html(severity)}</td>
                            <td>{escape_html(item.get('mitigation', ''))}</td>
                        </tr>
                    """
                html += "</tbody></table>"

            # Complexity Estimation
            complexity_est = arch_report.get('complexityEstimation', {})
            if complexity_est:
                html += f"""
                <h2>Complexity Estimation</h2>
                <div class="metric">
                    <p><strong>Function Points:</strong> {complexity_est.get('function_points', 'N/A')}</p>
                    <p><strong>Story Points:</strong> {complexity_est.get('story_points', 'N/A')}</p>
                    <p><strong>Effort Estimate:</strong> {complexity_est.get('effort_estimate_weeks', 'N/A')} weeks</p>
                </div>
                """
                top_complex = complexity_est.get('top_complex_requirements', [])
                if top_complex:
                    html += """<h3 style="margin-top: 15px; color: #34495e;">Most Complex Requirements</h3><ol>"""
                    for req in top_complex:
                        html += f"<li>{escape_html(req)}</li>"
                    html += "</ol>"

            # Novelty Assessment
            novelty = arch_report.get('noveltyAssessment', {})
            if novelty:
                breakdown = novelty.get('breakdown', {})
                html += f"""
                <h2>Novelty Assessment</h2>
                <div class="metric">
                    <p><strong>Novelty Score:</strong> {novelty.get('score', 'N/A')}/100</p>
                    <p><strong>Category:</strong> {escape_html(novelty.get('category', 'N/A'))}</p>
                """
                if breakdown:
                    html += f"""
                    <p><strong>Technical:</strong> {breakdown.get('technical', 0)}% | <strong>Domain:</strong> {breakdown.get('domain', 0)}% | <strong>Approach:</strong> {breakdown.get('approach', 0)}%</p>
                    """
                reasoning = novelty.get('reasoning', '')
                if reasoning:
                    html += f"""<p><em>{escape_html(reasoning)}</em></p>"""
                html += "</div>"
        
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
async def export_to_pdf(project_id: str):
    """Export project to PDF"""
    try:
        html_content = generate_html_report(project_id)
        loop = asyncio.get_event_loop()
        pdf_bytes = await loop.run_in_executor(None, lambda: HTML(string=html_content).write_pdf())
        return Response(
            content=pdf_bytes,
            media_type="application/pdf",
            headers={
                "Content-Disposition": f"attachment; filename=raag-report-{project_id}.pdf",
                "Content-Length": str(len(pdf_bytes))
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
