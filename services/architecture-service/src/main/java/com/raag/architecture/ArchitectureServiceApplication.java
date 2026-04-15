package com.raag.architecture;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.SourceStringReader;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@SpringBootApplication
public class ArchitectureServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ArchitectureServiceApplication.class, args);
    }
}

@Document(collection = "architectures")
class Architecture {
    @Id
    private String id;
    private String projectId;
    private String style;
    private List<Component> components;
    private String diagram;
    private double complexity;
    private Date createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getStyle() { return style; }
    public void setStyle(String style) { this.style = style; }

    public List<Component> getComponents() { return components; }
    public void setComponents(List<Component> components) { this.components = components; }

    public String getDiagram() { return diagram; }
    public void setDiagram(String diagram) { this.diagram = diagram; }

    public double getComplexity() { return complexity; }
    public void setComplexity(double complexity) { this.complexity = complexity; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}

class Component {
    private String name;
    private String type;
    private List<String> dependencies;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public List<String> getDependencies() { return dependencies; }
    public void setDependencies(List<String> dependencies) { this.dependencies = dependencies; }
}

class ArchitectureRequest {
    private String projectId;
    private String projectName;
    private String proposedStyle;
    private List<String> requirements;
    private String projectDescription;
    private String domain;

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }

    public String getProposedStyle() { return proposedStyle; }
    public void setProposedStyle(String proposedStyle) { this.proposedStyle = proposedStyle; }

    public List<String> getRequirements() { return requirements; }
    public void setRequirements(List<String> requirements) { this.requirements = requirements; }

    public String getProjectDescription() { return projectDescription; }
    public void setProjectDescription(String projectDescription) { this.projectDescription = projectDescription; }

    public String getDomain() { return domain; }
    public void setDomain(String domain) { this.domain = domain; }
}

class ArchitectureResponse {
    private String id;
    private String recommendedStyle;
    private String proposedStyle;
    private List<String> justification;
    private List<String> comparison;
    private double complexity;

    private List<RequirementQuality> qualityAnalysis;
    private List<RequirementGap> gapAnalysis;
    private DfdBundle dfd;
    private TraceabilityMatrixResult traceabilityMatrix;
    private List<RiskAssumptionItem> riskAndAssumptions;
    private ComplexityEstimate complexityEstimation;
    private NoveltyAssessmentResult noveltyAssessment;
    private List<GeneratedDiagram> additionalDiagrams;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getRecommendedStyle() { return recommendedStyle; }
    public void setRecommendedStyle(String recommendedStyle) { this.recommendedStyle = recommendedStyle; }

    public String getProposedStyle() { return proposedStyle; }
    public void setProposedStyle(String proposedStyle) { this.proposedStyle = proposedStyle; }

    public List<String> getJustification() { return justification; }
    public void setJustification(List<String> justification) { this.justification = justification; }

    public List<String> getComparison() { return comparison; }
    public void setComparison(List<String> comparison) { this.comparison = comparison; }

    public double getComplexity() { return complexity; }
    public void setComplexity(double complexity) { this.complexity = complexity; }

    public List<RequirementQuality> getQualityAnalysis() { return qualityAnalysis; }
    public void setQualityAnalysis(List<RequirementQuality> qualityAnalysis) { this.qualityAnalysis = qualityAnalysis; }

    public List<RequirementGap> getGapAnalysis() { return gapAnalysis; }
    public void setGapAnalysis(List<RequirementGap> gapAnalysis) { this.gapAnalysis = gapAnalysis; }

    public DfdBundle getDfd() { return dfd; }
    public void setDfd(DfdBundle dfd) { this.dfd = dfd; }

    public TraceabilityMatrixResult getTraceabilityMatrix() { return traceabilityMatrix; }
    public void setTraceabilityMatrix(TraceabilityMatrixResult traceabilityMatrix) { this.traceabilityMatrix = traceabilityMatrix; }

    public List<RiskAssumptionItem> getRiskAndAssumptions() { return riskAndAssumptions; }
    public void setRiskAndAssumptions(List<RiskAssumptionItem> riskAndAssumptions) { this.riskAndAssumptions = riskAndAssumptions; }

    public ComplexityEstimate getComplexityEstimation() { return complexityEstimation; }
    public void setComplexityEstimation(ComplexityEstimate complexityEstimation) { this.complexityEstimation = complexityEstimation; }

    public NoveltyAssessmentResult getNoveltyAssessment() { return noveltyAssessment; }
    public void setNoveltyAssessment(NoveltyAssessmentResult noveltyAssessment) { this.noveltyAssessment = noveltyAssessment; }

    public List<GeneratedDiagram> getAdditionalDiagrams() { return additionalDiagrams; }
    public void setAdditionalDiagrams(List<GeneratedDiagram> additionalDiagrams) { this.additionalDiagrams = additionalDiagrams; }
}

@Repository
interface ArchitectureRepository extends MongoRepository<Architecture, String> {
    Architecture findByProjectId(String projectId);
}

record RequirementQuality(
        String requirement,
        int score,
        boolean vagueness,
        @JsonProperty("missing_elements") List<String> missingElements,
        @JsonProperty("rewritten_requirement") String rewrittenRequirement
) {}

record RequirementGap(
        String gap,
        String severity,
        String suggestion
) {}

record DfdLevel(
        @JsonProperty("plantuml") String plantUml,
        String svg,
        List<String> nodes,
        List<String> edges
) {}

record DfdBundle(
        @JsonProperty("level_0") DfdLevel level0,
        @JsonProperty("level_1") DfdLevel level1
) {}

record TraceabilityEntry(
        String requirement,
        List<String> components
) {}

record HighDensityComponent(
        String component,
        @JsonProperty("requirement_count") int requirementCount
) {}

record TraceabilityMatrixResult(
        List<TraceabilityEntry> matrix,
        @JsonProperty("untraced_requirements") List<String> untracedRequirements,
        @JsonProperty("high_density_components") List<HighDensityComponent> highDensityComponents
) {}

record RiskAssumptionItem(
        String type,
        String description,
        String severity,
        String mitigation
) {}

record ComplexityEstimate(
        @JsonProperty("function_points") int functionPoints,
        @JsonProperty("story_points") int storyPoints,
        @JsonProperty("effort_estimate_weeks") int effortEstimateWeeks,
        @JsonProperty("top_complex_requirements") List<String> topComplexRequirements
) {}

record NoveltyBreakdown(
        int technical,
        int domain,
        int approach
) {}

record NoveltyAssessmentResult(
        int score,
        String category,
        NoveltyBreakdown breakdown,
        String reasoning
) {}

record GeneratedDiagram(
        String title,
        @JsonProperty("plantuml") String plantUml,
        String svg
) {}

@Service
class ArchitectureService {

    @Autowired
    private ArchitectureRepository repository;

    @Autowired
    private LLMClient llmClient;

    @Autowired
    private MongoTemplate mongoTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public ArchitectureResponse generateArchitectureReport(ArchitectureRequest request) {
        normalizeRequest(request);

        // Step 1: get architecture style and components (single sequential LLM call)
        Architecture arch = generateArchitecture(request);

        String description = safe(request.getProjectDescription(), "");
        List<String> requirements = request.getRequirements();
        String style = arch.getStyle();
        List<Component> components = arch.getComponents();
        String domain = safe(request.getDomain(), "General");
    String projectName = deriveProjectName(request.getProjectName(), description, request.getProjectId());

        // Step 2: launch all LLM-backed sections in parallel — each is bounded to 12s internally
        CompletableFuture<List<String>> justFuture = CompletableFuture.supplyAsync(
                () -> generateJustificationLLM(description, requirements, style));
        CompletableFuture<List<String>> compFuture = CompletableFuture.supplyAsync(
                () -> generateComparison(description, requirements, style, request.getProposedStyle()));
        CompletableFuture<List<RequirementQuality>> qualFuture = CompletableFuture.supplyAsync(
                () -> generateEnhancedQuality(request.getProjectId(), requirements));
        CompletableFuture<List<RequirementGap>> gapFuture = CompletableFuture.supplyAsync(
                () -> generateGapAnalysis(requirements));
        CompletableFuture<TraceabilityMatrixResult> traceFuture = CompletableFuture.supplyAsync(
                () -> generateTraceabilityMatrix(requirements, components));
        CompletableFuture<List<RiskAssumptionItem>> riskFuture = CompletableFuture.supplyAsync(
                () -> generateRiskAndAssumptions(description, requirements));
        CompletableFuture<ComplexityEstimate> complexFuture = CompletableFuture.supplyAsync(
                () -> generateComplexityEstimate(requirements));
        CompletableFuture<NoveltyAssessmentResult> noveltyFuture = CompletableFuture.supplyAsync(
                () -> generateNoveltyAssessment(description, domain, requirements));

        // DFD is computed locally — no LLM needed, completes immediately
    DfdBundle dfdBundle = generateDfdBundle(projectName, description, requirements, style, components, domain);
        List<String> externalEntities = extractExternalEntities(description, requirements);
        List<GeneratedDiagram> additionalDiagrams = generateAdditionalDiagrams(
                description, requirements, style, components, externalEntities
        );

        // Step 3: collect all parallel results (all bounded by 12s WebClient timeout + fallbacks)
        ArchitectureResponse response = new ArchitectureResponse();
        response.setId(arch.getId());
        response.setRecommendedStyle(style);
        response.setProposedStyle(safe(request.getProposedStyle(), "Monolithic"));
        response.setComplexity(arch.getComplexity());
        response.setJustification(justFuture.join());
        response.setComparison(compFuture.join());
        response.setQualityAnalysis(qualFuture.join());
        response.setGapAnalysis(gapFuture.join());
        response.setDfd(dfdBundle);
        response.setTraceabilityMatrix(traceFuture.join());
        response.setRiskAndAssumptions(riskFuture.join());
        response.setComplexityEstimation(complexFuture.join());
        response.setNoveltyAssessment(noveltyFuture.join());
        response.setAdditionalDiagrams(additionalDiagrams);

        if (dfdBundle != null && dfdBundle.level1() != null) {
            arch.setDiagram(dfdBundle.level1().plantUml());
            repository.save(arch);
        }

        // Persist the full architecture response for PDF export
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> reportDoc = objectMapper.convertValue(response, Map.class);
            reportDoc.put("projectId", request.getProjectId());
            reportDoc.put("savedAt", new Date());
            // Remove any prior report for this project
            mongoTemplate.remove(
                    new org.springframework.data.mongodb.core.query.Query(
                            org.springframework.data.mongodb.core.query.Criteria.where("projectId").is(request.getProjectId())
                    ),
                    "architecture_reports"
            );
            mongoTemplate.save(new org.bson.Document(reportDoc), "architecture_reports");
        } catch (Exception e) {
            // Non-critical — PDF export will just miss architecture data
            System.err.println("Failed to persist architecture report: " + e.getMessage());
        }

        return response;
    }

    private void normalizeRequest(ArchitectureRequest request) {
        if (request.getProjectId() == null || request.getProjectId().isBlank()) {
            request.setProjectId(UUID.randomUUID().toString());
        }

        if (request.getRequirements() == null) {
            request.setRequirements(List.of());
        } else {
            List<String> normalized = request.getRequirements().stream()
                    .filter(r -> r != null && !r.isBlank())
                    .map(String::trim)
                    .toList();
            request.setRequirements(normalized);
        }
    }

    private Architecture generateArchitecture(ArchitectureRequest request) {
        String recommendedStyle = callLLMForRecommendation(
                request.getProjectDescription(),
                request.getRequirements()
        );

        Architecture arch = new Architecture();
        arch.setProjectId(request.getProjectId());
        arch.setStyle(recommendedStyle);
        arch.setCreatedAt(new Date());

        List<Component> components = generateComponents(recommendedStyle, request.getRequirements());
        arch.setComponents(components);
        arch.setComplexity(calculateComplexity(components));
        arch.setDiagram(generateFallbackPlantUml(components));

        return repository.save(arch);
    }

    // ============================
    // ARCHITECTURE RECOMMENDATION LLM
    // ============================
    private String callLLMForRecommendation(String description, List<String> requirements) {

        String prompt = String.format("""
You are a software architect. Analyze the project and recommend the best architecture.

Project:
%s

Requirements:
%s

Candidate architectures:
Microservices, Monolithic, Serverless, Event-Driven, Layered, SOA, Hexagonal, CQRS, P2P

Evaluation criteria:
1. Scalability: Does the project need horizontal scaling or handle high concurrency?
2. Coupling: Do requirements suggest independent deployable units or tightly coupled modules?
3. Data flow: Is data flow event-based/streaming or request-response?
4. Team size: Complex distributed systems need larger teams.
5. Latency: Real-time requirements favor event-driven; simple CRUD favors monolithic.
6. Compliance: Regulated domains may need clear boundaries (microservices/hexagonal).

Return ONLY the architecture name. No explanation.
""", safe(description, ""), formatRequirements(requirements));

        Map<String, Object> response = llmClient.callCustomPrompt(prompt);
        String llmResult = extractResultString(response);

        if (llmResult == null || llmResult.isBlank()) {
            return mockRecommendation(prompt);
        }

        return llmResult.trim();
    }

    public List<String> generateJustificationLLM(String description, List<String> requirements, String recommended) {
        String prompt = String.format("""
Explain why %s architecture is the best fit for this project.

Project:
%s

Requirements:
%s

For each reason, reference specific requirements or project characteristics.
Cover these dimensions: scalability, maintainability, deployment flexibility, data management, and team productivity.
Give 3-5 specific, actionable reasons.
Return a JSON array of strings. No markdown, no prose outside the array.
""", recommended, safe(description, ""), formatRequirements(requirements));

        Map<String, Object> response = llmClient.callCustomPrompt(prompt);
        return extractResultList(response, mockJustification(recommended));
    }

    public List<String> generateComparison(
            String description,
            List<String> requirements,
            String recommended,
            String proposed
    ) {
        if (proposed == null || proposed.isBlank() || recommended.equalsIgnoreCase(proposed)) {
            return List.of("Proposed architecture matches recommendation.");
        }

        String prompt = String.format("""
Compare architectures:

Project:
%s

Requirements:
%s

Recommended: %s
Proposed: %s

Give EXACTLY 5 reasoning-based points why recommended is better.
Return JSON array.
""", safe(description, ""), formatRequirements(requirements), recommended, proposed);

        Map<String, Object> response = llmClient.callCustomPrompt(prompt);
        return extractResultList(response, mockComparison(recommended, proposed));
    }

    // ============================
    // MODULE 1: ENHANCED QUALITY
    // ============================
    private List<RequirementQuality> generateEnhancedQuality(String projectId, List<String> requirements) {
        Map<String, Object> response = llmClient.analyzeEnhancedQuality(requirements, projectId);
        List<Map<String, Object>> rows = llmClient.readListOfMaps(response.get("requirements"));

        if (rows.isEmpty()) {
            return requirements.stream()
                    .map(this::fallbackRequirementQuality)
                    .toList();
        }

        List<RequirementQuality> out = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            String requirement = safe(row.get("requirement"), "");
            if (requirement.isBlank()) {
                continue;
            }
            int score = intValue(row.get("score"), 70);
            boolean vagueness = boolValue(row.get("vagueness"), false);
            List<String> missing = stringList(row.get("missing_elements"));
            String rewritten = safe(row.get("rewritten_requirement"), requirement);

            out.add(new RequirementQuality(
                    requirement,
                    bounded(score, 0, 100),
                    vagueness,
                    missing,
                    rewritten
            ));
        }

        if (out.isEmpty()) {
            return requirements.stream().map(this::fallbackRequirementQuality).toList();
        }

        return out;
    }

    private RequirementQuality fallbackRequirementQuality(String requirement) {
        String lower = requirement.toLowerCase(Locale.ROOT);
        boolean vague = lower.contains("fast") || lower.contains("easy") || lower.contains("user-friendly") || lower.contains("etc");

        List<String> missing = new ArrayList<>();
        if (!lower.contains("user") && !lower.contains("system") && !lower.contains("service")) {
            missing.add("Actor");
        }
        if (!(lower.contains("shall") || lower.contains("must"))) {
            missing.add("Action");
        }
        if (!(lower.contains("when") || lower.contains("if") || lower.contains("within") || lower.matches(".*\\d+.*"))) {
            missing.add("Condition");
        }

        int score = 100;
        if (vague) score -= 20;
        score -= missing.size() * 12;
        if (!lower.matches(".*\\d+.*")) score -= 10;

        String actor = "The system";
        if (lower.startsWith("user ")) actor = "User";
        else if (lower.startsWith("users ")) actor = "Users";
        else if (lower.startsWith("admin ")) actor = "Admin";
        else if (lower.startsWith("administrator ")) actor = "Administrator";
        else if (lower.startsWith("customer ")) actor = "Customer";
        else if (lower.startsWith("client ")) actor = "Client";
        else if (lower.startsWith("operator ")) actor = "Operator";

        String action = requirement
                .replaceAll("(?i)^\\s*(the\\s+system|system|user|users|admin|administrator|customer|client|operator)\\s+(shall|must|should|can|will)\\s*", "")
                .replaceAll("(?i)\\b(fast|quick|easy|simple|user-friendly|robust|seamless|efficient|etc|and so on|better)\\b", "")
                .replaceAll("\\s+", " ")
                .trim();

        if (action.endsWith(".")) {
            action = action.substring(0, action.length() - 1).trim();
        }
        if (action.isBlank() || action.equalsIgnoreCase("be") || action.equalsIgnoreCase("is") || action.equalsIgnoreCase("are")) {
            action = "process the requested operation";
        }

        String rewritten = actor + " shall " + action
                + " when a valid request is received within 2 seconds for at least 1,000 concurrent users.";

        return new RequirementQuality(requirement, bounded(score, 0, 100), vague, missing, rewritten);
    }

    // ============================
    // MODULE 2: GAP ANALYSIS
    // ============================
    private List<RequirementGap> generateGapAnalysis(List<String> requirements) {
        Map<String, Object> response = llmClient.analyzeRequirementGaps(requirements);
        List<Map<String, Object>> rows = llmClient.readListOfMaps(response.get("gaps"));

        List<RequirementGap> out = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            String gap = safe(row.get("gap"), "");
            String severity = safe(row.get("severity"), "Medium");
            String suggestion = safe(row.get("suggestion"), "");
            if (!gap.isBlank() && !suggestion.isBlank()) {
                out.add(new RequirementGap(gap, severity, suggestion));
            }
        }

        if (!out.isEmpty()) {
            return out;
        }

        return List.of(
                new RequirementGap(
                        "No security requirements found",
                        "Critical",
                        "System shall implement JWT-based authentication and AES-256 encryption for sensitive data."
                )
        );
    }

    // ============================
    // MODULE 3: DFD LEVEL 0/1
    // ============================
    private DfdBundle generateDfdBundle(
            String projectName,
            String description,
            List<String> requirements,
            String architectureStyle,
            List<Component> components,
            String domain
    ) {
        List<String> externalEntities = extractExternalEntities(description, requirements);

        String level0Plant = generateDfdPlantUmlWithLlm(
                0,
                projectName,
                description,
                requirements,
                architectureStyle,
                externalEntities,
                domain
        );
        if (!isValidLevel0Dfd(level0Plant, externalEntities, projectName)) {
            level0Plant = buildLevel0PlantUml(architectureStyle, externalEntities, projectName);
        }

        List<String> level0Nodes = new ArrayList<>(externalEntities);
        level0Nodes.add("P0: " + projectName + " System");
        List<String> level0Edges = new ArrayList<>();
        for (String entity : externalEntities) {
            String lower = entity.toLowerCase(Locale.ROOT);
            if (lower.contains("admin")) {
                level0Edges.add(entity + " -> P0: Admin configuration/policies");
                level0Edges.add("P0 -> " + entity + ": Audit/operations status + control feedback");
            } else if (lower.contains("external") || lower.contains("gateway") || lower.contains("regulator")) {
                level0Edges.add(entity + " -> P0: External responses/data");
                level0Edges.add("P0 -> " + entity + ": Requests for inference/export/integration");
            } else {
                level0Edges.add(entity + " -> P0: Project details + requirements");
                level0Edges.add("P0 -> " + entity + ": Analysis results + recommendations + reports");
            }
        }

        DfdLevel level0 = new DfdLevel(
                level0Plant,
                renderSvg(level0Plant),
                level0Nodes,
                level0Edges
        );

        String level1Plant = generateDfdPlantUmlWithLlm(
                1,
                projectName,
                description,
                requirements,
                architectureStyle,
                externalEntities,
                domain
        );
        if (!isValidLevel1Dfd(level1Plant, externalEntities)) {
            level1Plant = buildLevel1FallbackPlantUml(projectName, description, requirements, externalEntities, domain);
        }

        List<String> derivedProcesses = deriveLevel1Processes(description, requirements, domain);
        List<String> derivedStores = deriveDataStores(description, requirements, domain);

        List<String> level1Nodes = new ArrayList<>();
        for (int i = 0; i < derivedProcesses.size(); i++) {
            level1Nodes.add("P" + (i + 1) + ": " + derivedProcesses.get(i));
        }
        for (int i = 0; i < derivedStores.size(); i++) {
            level1Nodes.add("D" + (i + 1) + ": " + derivedStores.get(i));
        }

        List<String> level1Edges = new ArrayList<>();
        if (!derivedProcesses.isEmpty()) {
            level1Edges.addAll(externalEntities.stream()
                    .map(e -> e + " -> P1")
                    .toList());
            for (int i = 1; i < derivedProcesses.size(); i++) {
                level1Edges.add("P" + i + " -> P" + (i + 1));
            }
            for (int i = 0; i < derivedStores.size(); i++) {
                int processIndex = Math.min(i + 1, derivedProcesses.size());
                level1Edges.add("P" + processIndex + " -> D" + (i + 1));
            }
            int lastProcess = derivedProcesses.size();
            level1Edges.addAll(externalEntities.stream()
                    .map(e -> "P" + lastProcess + " -> " + e)
                    .toList());
        }

        DfdLevel level1 = new DfdLevel(
                level1Plant,
                renderSvg(level1Plant),
        level1Nodes,
                level1Edges
        );

        return new DfdBundle(level0, level1);
    }

    private String generateDfdPlantUmlWithLlm(
            int level,
        String projectName,
            String description,
            List<String> requirements,
            String architectureStyle,
        List<String> externalEntities,
        String domain
    ) {
        String prompt = level == 0
        ? buildDfdLevel0Prompt(projectName, description, requirements, architectureStyle, externalEntities, domain)
        : buildDfdLevel1Prompt(projectName, description, requirements, architectureStyle, externalEntities, domain);

        try {
            Map<String, Object> response = llmClient.callCustomPrompt(prompt);
            String plant = extractPlantUmlFromResponse(response);
            if (isValidPlantUml(plant)) {
                return plant;
            }
        } catch (Exception ignored) {
            return null;
        }

        return null;
    }

    private boolean isValidLevel0Dfd(String plantUml, List<String> externalEntities, String projectName) {
        if (!isValidPlantUml(plantUml)) {
            return false;
        }

        String lower = plantUml.toLowerCase(Locale.ROOT);
        if (lower.contains("database ") || lower.contains("datastore") || lower.contains("storage ")) {
            return false;
        }
        if (Pattern.compile("\\bP[1-9]\\d*\\b", Pattern.CASE_INSENSITIVE).matcher(plantUml).find()) {
            return false;
        }
        String expectedProcessLabel = "P0: " + safe(projectName, "Project") + " System";
        if (!plantUml.contains(expectedProcessLabel)) {
            return false;
        }

        int processCount = 0;
        Matcher processMatcher = Pattern.compile("(?i)(rectangle|process|component)\\s+\"[^\"]*" + Pattern.quote(expectedProcessLabel) + "[^\"]*\"").matcher(plantUml);
        while (processMatcher.find()) {
            processCount++;
        }
        if (processCount != 1) {
            return false;
        }

        for (String entity : externalEntities) {
            String alias = alias(entity);
            boolean hasInput = plantUml.contains(alias + " --> P0") || plantUml.contains(alias + "-> P0") || plantUml.contains(alias + "--> P0");
            boolean hasOutput = plantUml.contains("P0 --> " + alias) || plantUml.contains("P0 -> " + alias) || plantUml.contains("P0 -->" + alias);
            if (!(hasInput && hasOutput)) {
                return false;
            }
        }

        return true;
    }

    private boolean isValidLevel1Dfd(String plantUml, List<String> externalEntities) {
        if (!isValidPlantUml(plantUml)) {
            return false;
        }

        Matcher processMatcher = Pattern.compile("\\bP([1-9]\\d*)\\b").matcher(plantUml);
        Set<String> processes = new LinkedHashSet<>();
        while (processMatcher.find()) {
            processes.add("P" + processMatcher.group(1));
        }
        if (processes.size() < 2) {
            return false;
        }

        for (String entity : externalEntities) {
            String entityAlias = alias(entity);
            if (!plantUml.contains(entityAlias)) {
                return false;
            }
        }

        Matcher edgeMatcher = Pattern.compile("([A-Za-z0-9_]+)\\s*-+>\\s*([A-Za-z0-9_]+)").matcher(plantUml);
        Map<String, Integer> incoming = new HashMap<>();
        Map<String, Integer> outgoing = new HashMap<>();
        int processToProcessEdges = 0;

        while (edgeMatcher.find()) {
            String src = edgeMatcher.group(1);
            String dst = edgeMatcher.group(2);
            if (src.matches("P[1-9]\\d*")) {
                outgoing.put(src, outgoing.getOrDefault(src, 0) + 1);
            }
            if (dst.matches("P[1-9]\\d*")) {
                incoming.put(dst, incoming.getOrDefault(dst, 0) + 1);
            }
            if (src.matches("P[1-9]\\d*") && dst.matches("P[1-9]\\d*")) {
                processToProcessEdges++;
            }
        }

        if (processToProcessEdges == 0) {
            return false;
        }

        int balancedProcesses = 0;
        for (String p : processes) {
            if (incoming.getOrDefault(p, 0) > 0 && outgoing.getOrDefault(p, 0) > 0) {
                balancedProcesses++;
            }
        }

        return balancedProcesses >= Math.max(2, processes.size() - 1);
    }

    private String buildDfdLevel0Prompt(
            String projectName,
            String description,
            List<String> requirements,
            String architectureStyle,
            List<String> externalEntities,
            String domain
    ) {
        return String.format("""
You are a professional System Analyst.
Generate Level 0 DFD in PlantUML based ONLY on the user's project idea.

Project Name:
%s

Project Description:
%s

Domain:
%s

Requirements:
%s

Architecture Style (context only):
%s

External Entities (seed list, refine if needed):
%s

STRICT RULES (must follow exactly):
1) Represent the whole system as ONE SINGLE process only.
2) Show ONLY external entities and input/output data flows.
3) Do NOT add internal subprocesses.
4) Do NOT add data stores.
5) Keep it high-level and abstract.
6) Name the main process exactly: "P0: %s System".
7) Do NOT include AI, RAAG internals, backend APIs, microservices, queues, or technical components.
8) Use real domain entities from project description.

Return only JSON:
{"plantuml_code":"@startuml ... @enduml"}

PlantUML must start with @startuml and end with @enduml.
""", safe(projectName, "Project"), safe(description, ""), safe(domain, "General"), formatRequirements(requirements), safe(architectureStyle, "Architecture"), String.join(", ", externalEntities), safe(projectName, "Project"));
    }

    private String buildDfdLevel1Prompt(
            String projectName,
            String description,
            List<String> requirements,
            String architectureStyle,
            List<String> externalEntities,
            String domain
    ) {
        return String.format("""
You are a professional System Analyst.
Generate Level 1 DFD in PlantUML by decomposing P0 for the same user project.

Project Name:
%s

Project Description:
%s

Domain:
%s

Requirements:
%s

Architecture Style (context only):
%s

External Entities to stay consistent with Level 0:
%s

STRICT RULES (must follow exactly):
1) Decompose P0: %s System into 4-6 meaningful domain-specific subprocesses (P1, P2, ...).
2) Each subprocess must show inputs and outputs.
3) Include data stores (D1, D2...) only when applicable.
4) Show proper data flow between subprocesses, external entities, and data stores.
5) Maintain consistency with Level 0 external entities.
6) Keep names practical and derived from user project context.
7) Do NOT include AI, RAAG internals, backend APIs, microservices, queues, or technical components.
8) Avoid duplicate flows and keep the diagram readable.

Return only JSON:
{"plantuml_code":"@startuml ... @enduml"}

PlantUML must start with @startuml and end with @enduml.
""", safe(projectName, "Project"), safe(description, ""), safe(domain, "General"), formatRequirements(requirements), safe(architectureStyle, "Architecture"), String.join(", ", externalEntities), safe(projectName, "Project"));
    }

    private List<String> extractExternalEntities(String description, List<String> requirements) {
        String text = (safe(description, "") + " " + String.join(" ", requirements)).toLowerCase(Locale.ROOT);
        Set<String> entities = new LinkedHashSet<>();

        if (containsAny(text, "student")) entities.add("Student");
        if (containsAny(text, "passenger")) entities.add("Passenger");
        if (containsAny(text, "patient")) entities.add("Patient");
        if (containsAny(text, "teacher", "faculty")) entities.add("Teacher");
        if (containsAny(text, "doctor")) entities.add("Doctor");
        if (containsAny(text, "admin", "administrator")) entities.add("Admin");
        if (containsAny(text, "user", "customer", "client")) entities.add("Project User");
        if (containsAny(text, "payment", "billing")) entities.add("Payment Gateway");
        if (containsAny(text, "government", "regulator", "compliance", "audit")) entities.add("Regulator");
        if (containsAny(text, "external", "third-party", "integration", "api")) entities.add("External Services");

        if (entities.isEmpty()) {
            entities.add("Project User");
            entities.add("Admin");
            entities.add("External Services");
        }

        return new ArrayList<>(entities);
    }

    private Graph<String, DefaultEdge> buildComponentGraph(List<Component> components) {
        Graph<String, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class);

        for (Component component : components) {
            graph.addVertex(component.getName());
        }

        for (Component component : components) {
            if (component.getDependencies() == null) {
                continue;
            }
            for (String dependency : component.getDependencies()) {
                if (dependency == null || dependency.isBlank()) {
                    continue;
                }
                graph.addVertex(dependency);
                if (!graph.containsEdge(dependency, component.getName())) {
                    graph.addEdge(dependency, component.getName());
                }
            }
        }

        if (graph.edgeSet().isEmpty()) {
            List<String> ordered = components.stream().map(Component::getName).toList();
            for (int i = 0; i < ordered.size() - 1; i++) {
                if (!graph.containsEdge(ordered.get(i), ordered.get(i + 1))) {
                    graph.addEdge(ordered.get(i), ordered.get(i + 1));
                }
            }
        }

        return graph;
    }

    private List<String> deriveDataStores(String description, List<String> requirements, String domain) {
        String text = (safe(description, "") + " " + String.join(" ", requirements) + " " + safe(domain, "")).toLowerCase(Locale.ROOT);
        Set<String> stores = new LinkedHashSet<>();

        if (containsAny(text, "health", "patient", "doctor", "clinic", "hospital", "ehr", "medical")) {
            stores.add("Patient Records DB");
            stores.add("Appointments DB");
            if (containsAny(text, "billing", "payment", "insurance")) stores.add("Billing DB");
            if (containsAny(text, "audit", "compliance", "hipaa")) stores.add("Compliance Audit DB");
        } else if (containsAny(text, "e-commerce", "ecommerce", "order", "product", "cart", "seller")) {
            stores.add("Customer DB");
            stores.add("Product Catalog DB");
            stores.add("Order DB");
            if (containsAny(text, "payment", "invoice", "billing")) stores.add("Payment DB");
        } else if (containsAny(text, "bank", "banking", "account", "transaction", "loan")) {
            stores.add("Customer Accounts DB");
            stores.add("Transactions DB");
            if (containsAny(text, "loan", "credit")) stores.add("Loans DB");
            stores.add("Compliance Audit DB");
        } else if (containsAny(text, "education", "school", "university", "student", "course", "teacher", "grade")) {
            stores.add("Student DB");
            stores.add("Course Catalog DB");
            stores.add("Enrollment DB");
            if (containsAny(text, "grade", "exam", "result")) stores.add("Grades DB");
        } else if (containsAny(text, "transport", "ticket", "passenger", "trip", "ride", "fare")) {
            stores.add("Passenger DB");
            stores.add("Booking DB");
            stores.add("Trip Schedule DB");
            if (containsAny(text, "fare", "payment", "billing")) stores.add("Fare Transactions DB");
        } else {
            stores.add("User DB");
            stores.add("Transactions DB");
            stores.add("Reports DB");
        }

        if (containsAny(text, "audit", "compliance", "log") && stores.stream().noneMatch(s -> s.toLowerCase(Locale.ROOT).contains("audit"))) {
            stores.add("Audit Log DB");
        }

        return new ArrayList<>(stores);
    }

    private String buildLevel0PlantUml(String architectureStyle, List<String> externalEntities, String projectName) {
        StringBuilder sb = new StringBuilder();
        sb.append("@startuml\n");
        sb.append("left to right direction\n");
        sb.append("skinparam shadowing false\n");
        sb.append("skinparam packageStyle rectangle\n");

        for (String entity : externalEntities) {
            sb.append("actor \"").append(escapePlant(entity)).append("\" as ").append(alias(entity)).append("\n");
        }

    sb.append("rectangle \"P0: ").append(escapePlant(projectName)).append(" System\" as P0\n");

        for (String entity : externalEntities) {
            String a = alias(entity);
            String lower = entity.toLowerCase(Locale.ROOT);
            if (lower.contains("admin")) {
                sb.append(a).append(" --> P0 : Admin configuration/policies\n");
                sb.append("P0 --> ").append(a).append(" : Audit/operations status + control feedback\n");
            } else if (lower.contains("external") || lower.contains("gateway") || lower.contains("regulator")) {
                sb.append(a).append(" --> P0 : External responses/data\n");
                sb.append("P0 --> ").append(a).append(" : Requests for inference/export/integration\n");
            } else {
                sb.append(a).append(" --> P0 : Project details + requirements\n");
                sb.append("P0 --> ").append(a).append(" : Analysis results + recommendations + reports\n");
            }
        }

        sb.append("@enduml\n");
        return sb.toString();
    }

    private String buildLevel1FallbackPlantUml(
            String projectName,
            String description,
            List<String> requirements,
            List<String> externalEntities,
            String domain
    ) {
        List<String> processes = deriveLevel1Processes(description, requirements, domain);
        List<String> stores = deriveDataStores(description, requirements, domain);

        StringBuilder sb = new StringBuilder();
        sb.append("@startuml\n");
        sb.append("left to right direction\n");
        sb.append("skinparam shadowing false\n");
        sb.append("skinparam componentStyle rectangle\n");
    sb.append("title ").append(escapePlant(projectName)).append(" DFD Level 1 - Functional Decomposition\n");

        for (String entity : externalEntities) {
            sb.append("actor \"").append(escapePlant(entity)).append("\" as ").append(alias(entity)).append("\n");
        }

        int pi = 1;
        List<String> processAliases = new ArrayList<>();
        for (String process : processes) {
            String processId = "P" + pi;
            sb.append("rectangle \"").append(processId).append(": ").append(escapePlant(process)).append("\" as ").append(processId).append("\n");
            processAliases.add(processId);
            pi++;
        }

        int di = 1;
        List<String> storeAliases = new ArrayList<>();
        for (String store : stores) {
            String storeId = "D" + di;
            sb.append("database \"").append(storeId).append(": ").append(escapePlant(store)).append("\" as ").append(storeId).append("\n");
            storeAliases.add(storeId);
            di++;
        }

        if (!processAliases.isEmpty()) {
            String first = processAliases.get(0);
            String last = processAliases.get(processAliases.size() - 1);

            for (String entity : externalEntities) {
                String a = alias(entity);
                String lower = entity.toLowerCase(Locale.ROOT);
                if (lower.contains("admin")) {
                    sb.append(a).append(" --> ").append(last).append(" : monitoring/control request\n");
                    sb.append(last).append(" --> ").append(a).append(" : status/feedback\n");
                } else {
                    sb.append(a).append(" --> ").append(first).append(" : input/request\n");
                    sb.append(last).append(" --> ").append(a).append(" : output/response\n");
                }
            }

            for (int i = 0; i < processAliases.size() - 1; i++) {
                sb.append(processAliases.get(i)).append(" --> ").append(processAliases.get(i + 1)).append(" : data flow\n");
            }

            for (int i = 0; i < storeAliases.size(); i++) {
                String p = processAliases.get(Math.min(i, processAliases.size() - 1));
                sb.append(p).append(" --> ").append(storeAliases.get(i)).append(" : read/write\n");
            }
        }

        sb.append("@enduml\n");
        return sb.toString();
    }

    private List<String> deriveLevel1Processes(String description, List<String> requirements, String domain) {
        String text = (safe(description, "") + " " + String.join(" ", requirements) + " " + safe(domain, "")).toLowerCase(Locale.ROOT);
        List<String> processes = new ArrayList<>();

        if (containsAny(text, "health", "patient", "doctor", "clinic", "hospital", "ehr", "medical")) {
            processes.add("Patient Management");
            processes.add("Appointment Scheduling");
            processes.add("Medical Records Handling");
            if (containsAny(text, "message", "communication", "notify")) processes.add("Patient Communication");
            if (containsAny(text, "billing", "payment", "insurance")) processes.add("Billing and Payments");
        } else if (containsAny(text, "e-commerce", "ecommerce", "order", "product", "cart", "seller")) {
            processes.add("Customer Account Management");
            processes.add("Catalog Management");
            processes.add("Order Processing");
            if (containsAny(text, "payment", "checkout", "invoice")) processes.add("Payment Processing");
            if (containsAny(text, "delivery", "shipping")) processes.add("Delivery Coordination");
        } else if (containsAny(text, "bank", "banking", "account", "transaction", "loan")) {
            processes.add("Customer Onboarding");
            processes.add("Account Management");
            processes.add("Transaction Processing");
            if (containsAny(text, "loan", "credit")) processes.add("Loan Management");
            processes.add("Compliance and Reporting");
        } else if (containsAny(text, "education", "school", "university", "student", "course", "teacher", "grade")) {
            processes.add("Student Registration");
            processes.add("Course Enrollment");
            processes.add("Class and Schedule Management");
            if (containsAny(text, "grade", "exam", "result")) processes.add("Assessment and Grading");
            if (containsAny(text, "report", "transcript")) processes.add("Academic Reporting");
        } else if (containsAny(text, "transport", "ticket", "passenger", "trip", "ride", "fare")) {
            processes.add("Passenger Registration");
            processes.add("Ticket Booking");
            processes.add("Trip Scheduling and Tracking");
            if (containsAny(text, "payment", "fare", "billing")) processes.add("Fare Payment Processing");
            if (containsAny(text, "compliance", "regulator", "audit")) processes.add("Regulatory Reporting");
        } else {
            processes.add("User Management");
            processes.add("Core Service Processing");
            processes.add("Transaction Handling");
            processes.add("Reporting and Notifications");
        }

        return processes.stream().distinct().limit(6).toList();
    }

    private String buildLevel1PlantUml(String style, Graph<String, DefaultEdge> graph, List<String> stores) {
        StringBuilder sb = new StringBuilder();
        sb.append("@startuml\n");
        sb.append("left to right direction\n");
        sb.append("skinparam shadowing false\n");
        sb.append("skinparam componentStyle rectangle\n");

        sb.append("title RAAG DFD Level 1 - Functional Decomposition\n");

        sb.append("actor \"Project User\" as E_USER\n");
        sb.append("actor \"Admin\" as E_ADMIN\n");
        sb.append("actor \"External Services\" as E_EXT\n");

        sb.append("rectangle \"P1: Capture Project Input\" as P1\n");
        sb.append("rectangle \"P2: Classify & Score Requirements\" as P2\n");
        sb.append("rectangle \"P3: Generate Architecture & DFD Suggestions\" as P3\n");
        sb.append("rectangle \"P4: Build Dashboard/Chat Responses\" as P4\n");
        sb.append("rectangle \"P5: Export Report\" as P5\n");
        sb.append("rectangle \"P6: Record Audit Trail\" as P6\n");

        sb.append("database \"D1: Project Repository\" as D1\n");
        sb.append("database \"D2: Analysis Repository\" as D2\n");
        sb.append("database \"D3: Audit Log Store\" as D3\n");

        sb.append("E_USER --> P1 : project metadata + requirements\n");
        sb.append("P1 --> D1 : save project\n");
        sb.append("P1 --> P2 : normalized requirements\n");

        sb.append("P2 --> E_EXT : inference request\n");
        sb.append("E_EXT --> P2 : inference response\n");
        sb.append("P2 --> D2 : classification + quality scores\n");
        sb.append("P2 --> P3 : classified requirements + constraints\n");

        sb.append("P3 --> E_EXT : architecture generation request\n");
        sb.append("E_EXT --> P3 : architecture generation response\n");
        sb.append("P3 --> D2 : architecture recommendation artifacts\n");

        sb.append("D1 --> P4 : project data\n");
        sb.append("D2 --> P4 : analysis data\n");
        sb.append("P4 --> E_USER : dashboard/chat response\n");

        sb.append("D1 --> P5 : export project dataset\n");
        sb.append("D2 --> P5 : export analysis dataset\n");
        sb.append("P5 --> E_EXT : render/export request\n");
        sb.append("E_EXT --> P5 : render/export response\n");
        sb.append("P5 --> E_USER : downloadable report\n");

        sb.append("P1 --> P6 : audit events\n");
        sb.append("P2 --> P6 : audit events\n");
        sb.append("P3 --> P6 : audit events\n");
        sb.append("P4 --> P6 : audit events\n");
        sb.append("P5 --> P6 : audit events\n");
        sb.append("P6 --> D3 : immutable audit record\n");

        sb.append("E_ADMIN --> P4 : monitoring request\n");
        sb.append("P4 --> E_ADMIN : monitoring response\n");
        sb.append("E_ADMIN --> P6 : audit control request\n");
        sb.append("P6 --> E_ADMIN : audit status\n");

        sb.append("@enduml\n");
        return sb.toString();
    }

    private String chooseWriterForStore(String store, List<String> vertices) {
        String lower = store.toLowerCase(Locale.ROOT);

        if (lower.contains("identity")) {
            return pickFirstMatching(vertices, List.of("auth", "identity", "user"));
        }
        if (lower.contains("audit")) {
            return pickFirstMatching(vertices, List.of("audit", "gateway"));
        }
        if (lower.contains("event")) {
            return pickFirstMatching(vertices, List.of("event", "stream", "broker"));
        }
        if (lower.contains("architecture")) {
            return pickFirstMatching(vertices, List.of("architecture", "analysis", "service"));
        }

        return vertices.isEmpty() ? null : vertices.get(0);
    }

    private String pickFirstMatching(List<String> vertices, List<String> keywords) {
        for (String vertex : vertices) {
            String l = vertex.toLowerCase(Locale.ROOT);
            for (String keyword : keywords) {
                if (l.contains(keyword)) {
                    return vertex;
                }
            }
        }
        return vertices.isEmpty() ? null : vertices.get(0);
    }

    private String renderSvg(String plantUml) {
        try {
            SourceStringReader reader = new SourceStringReader(plantUml);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            reader.outputImage(out, new FileFormatOption(FileFormat.SVG));
            return out.toString(StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"640\" height=\"70\">"
                    + "<text x=\"10\" y=\"35\" fill=\"#b00020\">DFD SVG rendering failed: "
                    + escapeXml(e.getMessage())
                    + "</text></svg>";
        }
    }

    // ============================
    // MODULE 4: TRACEABILITY MATRIX
    // ============================
    private TraceabilityMatrixResult generateTraceabilityMatrix(List<String> requirements, List<Component> components) {
        return fallbackTraceability(requirements, List.of());
    }

    private TraceabilityMatrixResult fallbackTraceability(List<String> requirements, List<String> componentNames) {
        List<String> canonicalComponents = List.of(
                "API Gateway",
                "Ingestion + LLM Analysis",
                "Quality + Architecture Services",
                "Frontend Dashboard & Chat UI",
                "Export Service",
                "Audit Service"
        );

        List<TraceabilityEntry> matrix = new ArrayList<>();
        Map<String, Integer> coverage = new HashMap<>();

        for (String component : canonicalComponents) {
            coverage.put(component, 0);
        }

        for (String requirement : requirements) {
            String lower = requirement.toLowerCase(Locale.ROOT);
            Set<String> mapped = new LinkedHashSet<>();

            // Core ingress always goes through API gateway
            mapped.add("API Gateway");

            if (containsAny(lower, "register", "login", "otp", "authenticate", "authorize", "rbac", "role", "permission")) {
                mapped.add("Ingestion + LLM Analysis");
                mapped.add("Quality + Architecture Services");
            }

            if (containsAny(lower, "requirement", "analysis", "classif", "quality", "architecture", "dfd", "fhir", "integration")) {
                mapped.add("Ingestion + LLM Analysis");
                mapped.add("Quality + Architecture Services");
            }

            if (containsAny(lower, "dashboard", "chat", "view", "display", "doctor", "patient", "schedule")) {
                mapped.add("Frontend Dashboard & Chat UI");
            }

            if (containsAny(lower, "report", "pdf", "csv", "export", "download")) {
                mapped.add("Export Service");
            }

            if (containsAny(lower, "audit", "compliance", "hipaa", "pci", "log", "immutable", "security", "encryption")) {
                mapped.add("Audit Service");
                mapped.add("Quality + Architecture Services");
            }

            if (containsAny(lower, "payment", "billing", "invoice")) {
                mapped.add("Ingestion + LLM Analysis");
                mapped.add("Quality + Architecture Services");
            }

            if (containsAny(lower, "uptime", "reliab", "recover", "backup", "availability", "performance")) {
                mapped.add("Quality + Architecture Services");
                mapped.add("Audit Service");
            }

            List<String> mappedList = new ArrayList<>(mapped);
            matrix.add(new TraceabilityEntry(requirement, mappedList));

            for (String component : mappedList) {
                coverage.put(component, coverage.getOrDefault(component, 0) + 1);
            }
        }

        List<String> untraced = matrix.stream()
                .filter(entry -> entry.components().isEmpty())
                .map(TraceabilityEntry::requirement)
                .toList();

        int threshold = Math.max(2, (int) Math.ceil(requirements.size() * 0.4));
        List<HighDensityComponent> highDensity = coverage.entrySet().stream()
                .filter(e -> e.getValue() >= threshold)
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .map(e -> new HighDensityComponent(e.getKey(), e.getValue()))
                .toList();

        return new TraceabilityMatrixResult(matrix, untraced, highDensity);
    }

    private boolean containsAny(String text, String... needles) {
        for (String needle : needles) {
            if (text.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    // ============================
    // MODULE 5: RISK & ASSUMPTIONS
    // ============================
    private List<RiskAssumptionItem> generateRiskAndAssumptions(String description, List<String> requirements) {
        Map<String, Object> response = llmClient.analyzeRiskAssumptions(description, requirements);
        List<Map<String, Object>> rows = llmClient.readListOfMaps(response.get("items"));

        List<RiskAssumptionItem> out = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            String type = safe(row.get("type"), "Risk");
            String desc = safe(row.get("description"), "");
            String severity = safe(row.get("severity"), "Medium");
            String mitigation = safe(row.get("mitigation"), "");
            if (!desc.isBlank() && !mitigation.isBlank()) {
                out.add(new RiskAssumptionItem(type, desc, severity, mitigation));
            }
        }

        if (!out.isEmpty()) {
            return out;
        }

        return List.of(
                new RiskAssumptionItem(
                        "Risk",
                        "Implicit compliance controls are missing in current requirement set.",
                        "High",
                        "Add mandatory compliance requirements and validate against regulatory checklists before release."
                )
        );
    }

    // ============================
    // MODULE 6: COMPLEXITY ESTIMATION
    // ============================
    private ComplexityEstimate generateComplexityEstimate(List<String> requirements) {
        Map<String, Object> response = llmClient.estimateComplexity(requirements);

        int functionPoints = intValue(response.get("function_points"), 0);
        int storyPoints = intValue(response.get("story_points"), 0);
        int effortWeeks = intValue(response.get("effort_estimate_weeks"), 0);
        List<String> topComplex = stringList(response.get("top_complex_requirements"));

        if (functionPoints > 0 || storyPoints > 0 || effortWeeks > 0) {
            return new ComplexityEstimate(functionPoints, storyPoints, effortWeeks, topComplex);
        }

        int fpFallback = Math.max(1, requirements.size() * 5);
        int spFallback = Math.max(1, (int) Math.round(fpFallback * 1.3));
        int weeksFallback = Math.max(1, (int) Math.ceil(spFallback / 12.0));

        List<String> topFallback = requirements.stream().limit(5).toList();
        return new ComplexityEstimate(fpFallback, spFallback, weeksFallback, topFallback);
    }

    // ============================
    // MODULE 7: NOVELTY ASSESSMENT
    // ============================
    private NoveltyAssessmentResult generateNoveltyAssessment(String description, String domain, List<String> requirements) {
        Map<String, Object> response = llmClient.assessNovelty(description, domain, requirements);

        int score = intValue(response.get("score"), -1);
        String category = safe(response.get("category"), "");
        String reasoning = safe(response.get("reasoning"), "");

        NoveltyBreakdown breakdown = new NoveltyBreakdown(0, 0, 0);
        Object breakdownObject = response.get("breakdown");
        if (breakdownObject instanceof Map<?, ?> map) {
            breakdown = new NoveltyBreakdown(
                    intValue(map.get("technical"), 0),
                    intValue(map.get("domain"), 0),
                    intValue(map.get("approach"), 0)
            );
        }

        if (score >= 0 && !category.isBlank()) {
            return new NoveltyAssessmentResult(score, category, breakdown, reasoning);
        }

        int fallbackScore = Math.min(100, 40 + (requirements.size() * 3));
        String fallbackCategory = fallbackScore >= 75 ? "Novel"
                : fallbackScore >= 60 ? "Moderately Novel"
                : fallbackScore >= 40 ? "Incremental"
                : "Conventional";

        NoveltyBreakdown fallbackBreakdown = new NoveltyBreakdown(
                Math.min(100, 35 + requirements.size() * 4),
                domain.equalsIgnoreCase("General") ? 42 : 58,
                55
        );

        return new NoveltyAssessmentResult(
                fallbackScore,
                fallbackCategory,
                fallbackBreakdown,
                "Novelty estimated from requirement breadth, domain complexity, and architectural approach."
        );
    }

    // ============================
    // MODULE 8: ADDITIONAL DIAGRAMS
    // ============================
    private List<GeneratedDiagram> generateAdditionalDiagrams(
            String description,
            List<String> requirements,
            String architectureStyle,
            List<Component> components,
            List<String> externalEntities
    ) {
        List<GeneratedDiagram> diagrams = new ArrayList<>();

        // 1. Component Diagram
        diagrams.add(buildComponentDiagram(architectureStyle, components));

        // 2. Use Case Diagram
        diagrams.add(buildUseCaseDiagram(requirements, externalEntities));

        // 3. Deployment Diagram
        diagrams.add(buildDeploymentDiagram(architectureStyle, components));

        // 4. Sequence Diagram
        diagrams.add(buildSequenceDiagram(architectureStyle, components));

        return diagrams;
    }

    private GeneratedDiagram buildComponentDiagram(String style, List<Component> components) {
        StringBuilder sb = new StringBuilder();
        sb.append("@startuml\n");
        sb.append("skinparam shadowing false\n");
        sb.append("skinparam componentStyle uml2\n");
        sb.append("title Component Diagram — ").append(escapePlant(safe(style, "Architecture"))).append("\n\n");

        // Group components by type
        Map<String, List<Component>> byType = new java.util.LinkedHashMap<>();
        for (Component c : components) {
            byType.computeIfAbsent(safe(c.getType(), "Service"), k -> new ArrayList<>()).add(c);
        }

        for (Map.Entry<String, List<Component>> entry : byType.entrySet()) {
            String type = entry.getKey();
            sb.append("package \"").append(escapePlant(type)).append("\" {\n");
            for (Component c : entry.getValue()) {
                sb.append("  [").append(escapePlant(c.getName())).append("] <<").append(escapePlant(type)).append(">>\n");
            }
            sb.append("}\n\n");
        }

        // Dependencies
        for (Component c : components) {
            if (c.getDependencies() == null) continue;
            for (String dep : c.getDependencies()) {
                if (dep == null || dep.isBlank()) continue;
                sb.append("[").append(escapePlant(dep)).append("] --> [").append(escapePlant(c.getName())).append("]\n");
            }
        }

        sb.append("@enduml\n");
        String puml = sb.toString();
        return new GeneratedDiagram("Component Diagram", puml, renderSvg(puml));
    }

    private GeneratedDiagram buildUseCaseDiagram(List<String> requirements, List<String> actors) {
        StringBuilder sb = new StringBuilder();
        sb.append("@startuml\n");
        sb.append("left to right direction\n");
        sb.append("skinparam shadowing false\n");
        sb.append("skinparam packageStyle rectangle\n");
        sb.append("title Use Case Diagram\n\n");

        // Actors
        for (String actor : actors) {
            sb.append("actor \"").append(escapePlant(actor)).append("\" as ").append(alias(actor)).append("\n");
        }
        sb.append("\n");

        sb.append("rectangle \"System\" {\n");

        // Convert requirements to use cases — abbreviate long ones
        int count = 0;
        List<String> ucAliases = new ArrayList<>();
        for (String req : requirements) {
            count++;
            String ucAlias = "UC" + count;
            ucAliases.add(ucAlias);

            // Extract a short label: take first meaningful clause, max 60 chars
            String label = req.replaceAll("(?i)^\\s*(the\\s+system\\s+)?(shall|must|should|will|can)\\s+", "").trim();
            if (label.length() > 60) {
                label = label.substring(0, 57) + "...";
            }
            if (label.isBlank()) label = "Requirement " + count;

            sb.append("  usecase \"").append(escapePlant(label)).append("\" as ").append(ucAlias).append("\n");
        }
        sb.append("}\n\n");

        // Connect actors to use cases — primary actor to all, others to subset
        if (!actors.isEmpty()) {
            String primaryAlias = alias(actors.get(0));
            for (String ucAlias : ucAliases) {
                sb.append(primaryAlias).append(" --> ").append(ucAlias).append("\n");
            }
            // Secondary actors connect to a relevant subset
            for (int i = 1; i < actors.size(); i++) {
                String actorAlias = alias(actors.get(i));
                String actorLower = actors.get(i).toLowerCase(Locale.ROOT);
                for (int j = 0; j < requirements.size(); j++) {
                    String reqLower = requirements.get(j).toLowerCase(Locale.ROOT);
                    boolean relevant = false;
                    if (actorLower.contains("admin") && (reqLower.contains("admin") || reqLower.contains("manage") || reqLower.contains("config"))) relevant = true;
                    if (actorLower.contains("payment") && (reqLower.contains("payment") || reqLower.contains("billing") || reqLower.contains("invoice"))) relevant = true;
                    if (actorLower.contains("external") && (reqLower.contains("integrat") || reqLower.contains("third-party") || reqLower.contains("external"))) relevant = true;
                    if (actorLower.contains("regulator") && (reqLower.contains("compliance") || reqLower.contains("audit") || reqLower.contains("regulat"))) relevant = true;
                    if (relevant) {
                        sb.append(actorAlias).append(" --> ").append(ucAliases.get(j)).append("\n");
                    }
                }
            }
        }

        sb.append("@enduml\n");
        String puml = sb.toString();
        return new GeneratedDiagram("Use Case Diagram", puml, renderSvg(puml));
    }

    private GeneratedDiagram buildDeploymentDiagram(String style, List<Component> components) {
        StringBuilder sb = new StringBuilder();
        sb.append("@startuml\n");
        sb.append("skinparam shadowing false\n");
        sb.append("title Deployment Diagram — ").append(escapePlant(safe(style, "Architecture"))).append("\n\n");

        String normalized = safe(style, "Monolithic").toLowerCase(Locale.ROOT);

        if (normalized.contains("micro") || normalized.contains("soa")) {
            // Each service gets its own container node
            sb.append("node \"Load Balancer\" as LB\n\n");
            for (Component c : components) {
                String cAlias = alias(c.getName());
                String nodeType = c.getType() != null && c.getType().toLowerCase(Locale.ROOT).contains("gateway") ? "node" : "node";
                sb.append(nodeType).append(" \"Container: ").append(escapePlant(c.getName())).append("\" as ").append(cAlias).append(" {\n");
                sb.append("  artifact \"").append(escapePlant(c.getName())).append("\" <<").append(escapePlant(safe(c.getType(), "Service"))).append(">>\n");
                sb.append("}\n");
            }
            sb.append("\ncloud \"Message Broker\" as MQ\n");
            sb.append("database \"Primary Database\" as DB\n");
            sb.append("database \"Cache\" as Cache\n\n");

            // Connect LB to gateway
            components.stream()
                    .filter(c -> c.getType() != null && c.getType().toLowerCase(Locale.ROOT).contains("gateway"))
                    .findFirst()
                    .ifPresent(gw -> sb.append("LB --> ").append(alias(gw.getName())).append("\n"));

            // Connect services
            for (Component c : components) {
                if (c.getDependencies() == null) continue;
                for (String dep : c.getDependencies()) {
                    if (dep == null || dep.isBlank()) continue;
                    sb.append(alias(dep)).append(" --> ").append(alias(c.getName())).append("\n");
                }
            }

            // Connect some services to DB/MQ
            for (Component c : components) {
                String lower = c.getName().toLowerCase(Locale.ROOT);
                if (lower.contains("service") || lower.contains("worker") || lower.contains("layer") || lower.contains("application")) {
                    sb.append(alias(c.getName())).append(" --> DB\n");
                    break;
                }
            }
            components.stream()
                    .filter(c -> {
                        String l = c.getName().toLowerCase(Locale.ROOT);
                        return l.contains("event") || l.contains("notification") || l.contains("worker") || l.contains("bus");
                    })
                    .findFirst()
                    .ifPresent(c -> sb.append(alias(c.getName())).append(" --> MQ\n"));

        } else if (normalized.contains("serverless")) {
            sb.append("cloud \"Cloud Provider\" {\n");
            sb.append("  node \"API Gateway\" as APIGW\n");
            for (Component c : components) {
                if (c.getType() != null && c.getType().toLowerCase(Locale.ROOT).contains("function")) {
                    sb.append("  node \"Lambda: ").append(escapePlant(c.getName())).append("\" as ").append(alias(c.getName())).append("\n");
                }
            }
            sb.append("  database \"Managed DB\" as DB\n");
            sb.append("  storage \"Object Storage\" as S3\n");
            sb.append("}\n\n");
            sb.append("actor \"Client\" as Client\n");
            sb.append("Client --> APIGW\n");
            for (Component c : components) {
                if (c.getType() != null && c.getType().toLowerCase(Locale.ROOT).contains("function")) {
                    sb.append("APIGW --> ").append(alias(c.getName())).append("\n");
                    sb.append(alias(c.getName())).append(" --> DB\n");
                }
            }

        } else if (normalized.contains("event")) {
            sb.append("node \"Load Balancer\" as LB\n");
            sb.append("queue \"Event Bus / Message Broker\" as EB\n\n");
            for (Component c : components) {
                sb.append("node \"").append(escapePlant(c.getName())).append("\" as ").append(alias(c.getName())).append("\n");
            }
            sb.append("\ndatabase \"Event Store\" as ES\n");
            sb.append("database \"Read DB\" as RDB\n\n");

            components.stream()
                    .filter(c -> c.getType() != null && c.getType().toLowerCase(Locale.ROOT).contains("gateway"))
                    .findFirst()
                    .ifPresent(gw -> sb.append("LB --> ").append(alias(gw.getName())).append("\n"));

            for (Component c : components) {
                String lower = c.getName().toLowerCase(Locale.ROOT);
                if (lower.contains("bus") || lower.contains("event")) {
                    sb.append(alias(c.getName())).append(" --> EB\n");
                } else if (lower.contains("command")) {
                    sb.append(alias(c.getName())).append(" --> EB\n");
                    sb.append(alias(c.getName())).append(" --> ES\n");
                } else if (lower.contains("query") || lower.contains("projection")) {
                    sb.append("EB --> ").append(alias(c.getName())).append("\n");
                    sb.append(alias(c.getName())).append(" --> RDB\n");
                } else if (lower.contains("worker") || lower.contains("notification")) {
                    sb.append("EB --> ").append(alias(c.getName())).append("\n");
                }
            }

        } else {
            // Monolithic / Layered
            sb.append("node \"Application Server\" {\n");
            for (Component c : components) {
                sb.append("  artifact \"").append(escapePlant(c.getName())).append("\" as ").append(alias(c.getName())).append("\n");
            }
            sb.append("}\n\n");
            sb.append("database \"Database\" as DB\n");
            sb.append("actor \"Client\" as Client\n\n");

            components.stream().findFirst()
                    .ifPresent(c -> sb.append("Client --> ").append(alias(c.getName())).append("\n"));

            for (Component c : components) {
                if (c.getDependencies() == null) continue;
                for (String dep : c.getDependencies()) {
                    if (dep == null || dep.isBlank()) continue;
                    sb.append(alias(dep)).append(" --> ").append(alias(c.getName())).append("\n");
                }
            }
            // Last component/layer connects to DB
            if (!components.isEmpty()) {
                sb.append(alias(components.get(components.size() - 1).getName())).append(" --> DB\n");
            }
        }

        sb.append("@enduml\n");
        String puml = sb.toString();
        return new GeneratedDiagram("Deployment Diagram", puml, renderSvg(puml));
    }

    private GeneratedDiagram buildSequenceDiagram(String style, List<Component> components) {
        StringBuilder sb = new StringBuilder();
        sb.append("@startuml\n");
        sb.append("skinparam shadowing false\n");
        sb.append("title Request Flow — ").append(escapePlant(safe(style, "Architecture"))).append("\n\n");

        // Participants
        sb.append("actor \"User\" as User\n");
        for (Component c : components) {
            String stereotype = safe(c.getType(), "Service");
            sb.append("participant \"").append(escapePlant(c.getName())).append("\" as ").append(alias(c.getName())).append(" <<").append(escapePlant(stereotype)).append(">>\n");
        }
        sb.append("database \"Database\" as DB\n\n");

        // Build a typical request flow through the dependency chain
        if (!components.isEmpty()) {
            Component entry = components.get(0); // first component is typically gateway/entry point
            sb.append("User -> ").append(alias(entry.getName())).append(" : HTTP Request\n");
            sb.append("activate ").append(alias(entry.getName())).append("\n");

            // Walk the dependency chain
            Set<String> activated = new LinkedHashSet<>();
            activated.add(alias(entry.getName()));
            List<String> callStack = new ArrayList<>();
            callStack.add(alias(entry.getName()));

            for (int i = 1; i < components.size(); i++) {
                Component c = components.get(i);
                // Find which already-activated component calls this one
                String caller = null;
                if (c.getDependencies() != null) {
                    for (String dep : c.getDependencies()) {
                        if (dep != null && activated.contains(alias(dep))) {
                            caller = alias(dep);
                            break;
                        }
                    }
                }
                if (caller == null) {
                    caller = callStack.get(callStack.size() - 1);
                }

                String callee = alias(c.getName());
                sb.append(caller).append(" -> ").append(callee).append(" : process\n");
                sb.append("activate ").append(callee).append("\n");
                activated.add(callee);
                callStack.add(callee);
            }

            // Last component queries DB
            String last = callStack.get(callStack.size() - 1);
            sb.append(last).append(" -> DB : query/persist\n");
            sb.append("DB --> ").append(last).append(" : result\n");

            // Return responses back up the chain
            for (int i = callStack.size() - 1; i >= 1; i--) {
                String callee = callStack.get(i);
                String caller = callStack.get(i - 1);
                sb.append(callee).append(" --> ").append(caller).append(" : response\n");
                sb.append("deactivate ").append(callee).append("\n");
            }

            sb.append(alias(entry.getName())).append(" --> User : HTTP Response\n");
            sb.append("deactivate ").append(alias(entry.getName())).append("\n");
        }

        sb.append("@enduml\n");
        String puml = sb.toString();
        return new GeneratedDiagram("Sequence Diagram", puml, renderSvg(puml));
    }

    // ============================
    // HELPERS
    // ============================
    private List<Component> generateComponents(String style, List<String> requirements) {
        String normalized = safe(style, "Monolithic").toLowerCase(Locale.ROOT);
        List<Component> components = new ArrayList<>();

        if (normalized.contains("micro")) {
            components.add(createComponent("API Gateway", "Gateway", List.of()));
            components.add(createComponent("Auth Service", "Service", List.of("API Gateway")));
            components.add(createComponent("Requirement Service", "Service", List.of("API Gateway", "Auth Service")));
            components.add(createComponent("Analysis Service", "Service", List.of("Requirement Service")));
            components.add(createComponent("Architecture Service", "Service", List.of("Analysis Service")));
            components.add(createComponent("Audit Service", "Service", List.of("API Gateway")));
            components.add(createComponent("Notification Service", "Service", List.of("Architecture Service")));
        } else if (normalized.contains("event")) {
            components.add(createComponent("API Gateway", "Gateway", List.of()));
            components.add(createComponent("Event Bus", "Messaging", List.of("API Gateway")));
            components.add(createComponent("Command Service", "Service", List.of("Event Bus")));
            components.add(createComponent("Query Service", "Service", List.of("Event Bus")));
            components.add(createComponent("Projection Worker", "Worker", List.of("Event Bus")));
            components.add(createComponent("Notification Worker", "Worker", List.of("Event Bus")));
        } else if (normalized.contains("serverless")) {
            components.add(createComponent("API Gateway", "Gateway", List.of()));
            components.add(createComponent("Auth Function", "Function", List.of("API Gateway")));
            components.add(createComponent("Analysis Function", "Function", List.of("API Gateway")));
            components.add(createComponent("Architecture Function", "Function", List.of("Analysis Function")));
        } else {
            components.add(createComponent("Main Application", "Monolith", List.of()));
            components.add(createComponent("Data Access Layer", "Layer", List.of("Main Application")));
            components.add(createComponent("Reporting Module", "Layer", List.of("Main Application")));
        }

        if (requirements != null && requirements.stream().anyMatch(r -> r.toLowerCase(Locale.ROOT).contains("chat"))) {
            components.add(createComponent("Chatbot Service", "Service", List.of("API Gateway")));
        }

        return components;
    }

    private Component createComponent(String name, String type, List<String> deps) {
        Component component = new Component();
        component.setName(name);
        component.setType(type);
        component.setDependencies(deps);
        return component;
    }

    private double calculateComplexity(List<Component> components) {
        int dependencyCount = components.stream()
                .map(Component::getDependencies)
                .filter(list -> list != null)
                .mapToInt(List::size)
                .sum();

        double raw = components.size() * 1.2 + dependencyCount * 0.45;
        return Math.round(raw * 10.0) / 10.0;
    }

    private String generateFallbackPlantUml(List<Component> components) {
        StringBuilder sb = new StringBuilder();
        sb.append("@startuml\n");
        for (Component component : components) {
            sb.append("component \"").append(escapePlant(component.getName())).append("\" as ").append(alias(component.getName())).append("\n");
        }
        for (Component component : components) {
            if (component.getDependencies() == null) continue;
            for (String dep : component.getDependencies()) {
                sb.append(alias(dep)).append(" --> ").append(alias(component.getName())).append("\n");
            }
        }
        sb.append("@enduml\n");
        return sb.toString();
    }

    private String formatRequirements(List<String> requirements) {
        if (requirements == null || requirements.isEmpty()) {
            return "- No requirements provided";
        }
        return "- " + String.join("\n- ", requirements);
    }

    private String deriveProjectName(String incomingProjectName, String description, String projectId) {
        String candidate = safe(incomingProjectName, "");
        if (!candidate.isBlank()) {
            return candidate;
        }

        String fromDescription = safe(description, "").trim();
        if (!fromDescription.isBlank()) {
            String[] tokens = fromDescription.split("\\s+");
            StringBuilder sb = new StringBuilder();
            int count = 0;
            for (String t : tokens) {
                String cleaned = t.replaceAll("[^A-Za-z0-9]", "");
                if (cleaned.isBlank()) continue;
                if (count > 0) sb.append(" ");
                sb.append(cleaned.substring(0, 1).toUpperCase(Locale.ROOT)).append(cleaned.substring(1));
                count++;
                if (count == 3) break;
            }
            if (!sb.isEmpty()) {
                return sb.toString();
            }
        }

        String id = safe(projectId, "Project");
        if (id.length() > 12) {
            id = id.substring(0, 12);
        }
        return "Project " + id;
    }

    private String extractResultString(Map<String, Object> response) {
        try {
            Object result = response.get("result");
            if (result instanceof String str && !str.isBlank()) {
                return str;
            }
            if (result instanceof List<?> list && !list.isEmpty() && list.get(0) != null) {
                String first = list.get(0).toString();
                if (!first.isBlank()) {
                    return first;
                }
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }

    private String extractPlantUmlFromResponse(Map<String, Object> response) {
        if (response == null || response.isEmpty()) {
            return null;
        }

        Object direct = response.get("plantuml_code");
        if (direct instanceof String s && isValidPlantUml(s)) {
            return s.trim();
        }

        Object result = response.get("result");
        if (result instanceof Map<?, ?> map) {
            Object code = map.get("plantuml_code");
            if (code instanceof String s && isValidPlantUml(s)) {
                return s.trim();
            }
            Object nestedResult = map.get("result");
            if (nestedResult instanceof String s) {
                String extracted = extractPlantUmlFromText(s);
                if (isValidPlantUml(extracted)) {
                    return extracted;
                }
            }
        }

        if (result instanceof String textResult) {
            String extracted = extractPlantUmlFromText(textResult);
            if (isValidPlantUml(extracted)) {
                return extracted;
            }
        }

        String fallbackText = extractResultString(response);
        return extractPlantUmlFromText(fallbackText);
    }

    private String extractPlantUmlFromText(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        String trimmed = raw.trim();
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> parsed = objectMapper.readValue(trimmed, Map.class);
                String code = safe(parsed.get("plantuml_code"), "");
                if (isValidPlantUml(code)) {
                    return code;
                }
            } catch (Exception ignored) {
                // fall through to regex extraction
            }
        }

        Matcher matcher = Pattern.compile("(@startuml[\\s\\S]*?@enduml)", Pattern.CASE_INSENSITIVE).matcher(trimmed);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        return null;
    }

    private boolean isValidPlantUml(String plantUml) {
        if (plantUml == null || plantUml.isBlank()) {
            return false;
        }
        String lower = plantUml.toLowerCase(Locale.ROOT);
        return lower.contains("@startuml") && lower.contains("@enduml");
    }

    private List<String> extractResultList(Map<String, Object> response, List<String> fallback) {
        try {
            Object result = response.get("result");
            if (result instanceof List<?> list) {
                List<String> parsed = new ArrayList<>();
                for (Object item : list) {
                    if (item != null && !item.toString().isBlank()) {
                        parsed.add(item.toString());
                    }
                }
                if (!parsed.isEmpty()) {
                    return parsed;
                }
            }

            if (result instanceof String str && !str.isBlank()) {
                return List.of(str);
            }
        } catch (Exception e) {
            return List.of("LLM response failed, fallback applied");
        }
        return fallback;
    }

    private int intValue(Object value, int fallback) {
        try {
            if (value instanceof Number n) {
                return n.intValue();
            }
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception e) {
            return fallback;
        }
    }

    private boolean boolValue(Object value, boolean fallback) {
        if (value instanceof Boolean b) {
            return b;
        }
        if (value instanceof Number n) {
            return n.intValue() != 0;
        }
        if (value instanceof String s) {
            return s.equalsIgnoreCase("true") || s.equalsIgnoreCase("yes") || s.equals("1");
        }
        return fallback;
    }

    private int bounded(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private String safe(Object value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String out = value.toString().trim();
        return out.isBlank() ? fallback : out;
    }

    private List<String> stringList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (Object item : list) {
            if (item == null) continue;
            String str = item.toString().trim();
            if (!str.isBlank()) {
                out.add(str);
            }
        }
        return out;
    }

    private String alias(String name) {
        String cleaned = name.replaceAll("[^A-Za-z0-9]", "_");
        if (cleaned.isBlank()) {
            return "N" + Math.abs(name.hashCode());
        }
        if (Character.isDigit(cleaned.charAt(0))) {
            return "N_" + cleaned;
        }
        return cleaned;
    }

    private String escapePlant(String input) {
        return input.replace("\"", "'");
    }

    private String escapeXml(String input) {
        if (input == null) {
            return "unknown";
        }
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    public String renderDiagramPublic(String plantUml) {
        return renderSvg(plantUml);
    }

    // ============================
    // MOCKS FOR FALLBACK
    // ============================
    private String mockRecommendation(String prompt) {
        String p = prompt.toLowerCase(Locale.ROOT);

        if (p.contains("event") || p.contains("real-time")) return "Event-Driven";
        if (p.contains("scale") || p.contains("millions")) return "Microservices";

        return "Monolithic";
    }

    private List<String> mockJustification(String recommended) {
        if (recommended.equalsIgnoreCase("Event-Driven")) {
            return List.of(
                    "The project involves asynchronous or real-time workflows",
                    "Loose coupling improves scalability and flexibility",
                    "Better handling of distributed event processing"
            );
        }
        return List.of("Architecture aligns with system requirements");
    }

    private List<String> mockComparison(String recommended, String proposed) {
        return List.of(
                recommended + " better supports scalability for this project than " + proposed,
                recommended + " aligns better with system workflow requirements",
                recommended + " improves modularity and decoupling compared to " + proposed,
                recommended + " handles performance needs more effectively",
                recommended + " is more suitable for future system growth"
        );
    }
}

@RestController
@RequestMapping("/")
class ArchitectureController {

    @Autowired
    private ArchitectureService service;

    @PostMapping("/generate-architecture")
    public ArchitectureResponse generateArchitecture(@RequestBody ArchitectureRequest request) {
        return service.generateArchitectureReport(request);
    }

    @PostMapping("/generate-architecture/advanced")
    public ArchitectureResponse generateArchitectureAdvanced(@RequestBody ArchitectureRequest request) {
        return service.generateArchitectureReport(request);
    }

    record RenderDiagramRequest(String code) {}
    record RenderDiagramResponse(String svg, String code) {}

    @PostMapping("/render-diagram")
    public RenderDiagramResponse renderDiagram(@RequestBody RenderDiagramRequest request) {
        String plantUml = request.code() == null ? "" : request.code().trim();
        if (plantUml.isEmpty()) {
            return new RenderDiagramResponse(
                "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"200\" height=\"40\">"
                + "<text x=\"10\" y=\"25\" fill=\"#b00020\">No diagram code provided.</text></svg>",
                plantUml
            );
        }
        String svg = service.renderDiagramPublic(plantUml);
        return new RenderDiagramResponse(svg, plantUml);
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok", "service", "architecture");
    }
}
