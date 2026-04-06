package com.raag.architecture;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    private String proposedStyle;
    private List<String> requirements;
    private String projectDescription;
    private String domain;

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

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

@Service
class ArchitectureService {

    @Autowired
    private ArchitectureRepository repository;

    @Autowired
    private LLMClient llmClient;

    public ArchitectureResponse generateArchitectureReport(ArchitectureRequest request) {
        normalizeRequest(request);

        Architecture arch = generateArchitecture(request);

        ArchitectureResponse response = new ArchitectureResponse();
        response.setId(arch.getId());
        response.setRecommendedStyle(arch.getStyle());
        response.setProposedStyle(safe(request.getProposedStyle(), "Monolithic"));
        response.setComplexity(arch.getComplexity());

        String description = safe(request.getProjectDescription(), "");
        List<String> requirements = request.getRequirements();

        response.setJustification(generateJustificationLLM(description, requirements, arch.getStyle()));
        response.setComparison(generateComparison(description, requirements, arch.getStyle(), request.getProposedStyle()));

        response.setQualityAnalysis(generateEnhancedQuality(request.getProjectId(), requirements));
        response.setGapAnalysis(generateGapAnalysis(requirements));

        DfdBundle dfdBundle = generateDfdBundle(description, requirements, arch.getStyle(), arch.getComponents());
        response.setDfd(dfdBundle);

        if (dfdBundle != null && dfdBundle.level1() != null) {
            arch.setDiagram(dfdBundle.level1().plantUml());
            repository.save(arch);
        }

        response.setTraceabilityMatrix(generateTraceabilityMatrix(requirements, arch.getComponents()));
        response.setRiskAndAssumptions(generateRiskAndAssumptions(description, requirements));
        response.setComplexityEstimation(generateComplexityEstimate(requirements));
        response.setNoveltyAssessment(generateNoveltyAssessment(description, safe(request.getDomain(), "General"), requirements));

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
You are a software architect.

Project:
%s

Requirements:
%s

Choose best architecture from:
Microservices, Monolithic, Serverless, Event-Driven, Layered, SOA, Hexagonal, CQRS, P2P

Return only one architecture name.
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
Explain why %s is best for this project:

Project:
%s

Requirements:
%s

Give 3-5 specific reasons.
Return JSON array.
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
            String description,
            List<String> requirements,
            String architectureStyle,
            List<Component> components
    ) {
        List<String> externalEntities = extractExternalEntities(description, requirements);

        String level0Plant = buildLevel0PlantUml(architectureStyle, externalEntities);
        List<String> level0Nodes = new ArrayList<>(externalEntities);
        level0Nodes.add("System");
        List<String> level0Edges = externalEntities.stream()
                .map(entity -> entity + " -> System")
                .collect(Collectors.toCollection(ArrayList::new));

        DfdLevel level0 = new DfdLevel(
                level0Plant,
                renderSvg(level0Plant),
                level0Nodes,
                level0Edges
        );

        Graph<String, DefaultEdge> graph = buildComponentGraph(components);
        List<String> stores = deriveDataStores(requirements);

        String level1Plant = buildLevel1PlantUml(architectureStyle, graph, stores);

        List<String> level1Edges = graph.edgeSet().stream()
                .map(edge -> graph.getEdgeSource(edge) + " -> " + graph.getEdgeTarget(edge))
                .collect(Collectors.toCollection(ArrayList::new));

        DfdLevel level1 = new DfdLevel(
                level1Plant,
                renderSvg(level1Plant),
                graph.vertexSet().stream().sorted().collect(Collectors.toCollection(ArrayList::new)),
                level1Edges
        );

        return new DfdBundle(level0, level1);
    }

    private List<String> extractExternalEntities(String description, List<String> requirements) {
        String text = (safe(description, "") + " " + String.join(" ", requirements)).toLowerCase(Locale.ROOT);
        Set<String> entities = new LinkedHashSet<>();

        if (text.contains("admin")) entities.add("Admin");
        if (text.contains("user") || text.contains("customer") || text.contains("client")) entities.add("End User");
        if (text.contains("payment") || text.contains("billing")) entities.add("Payment Gateway");
        if (text.contains("third-party") || text.contains("external")) entities.add("External Service");
        if (text.contains("compliance") || text.contains("regulatory") || text.contains("audit")) entities.add("Regulator");

        if (entities.isEmpty()) {
            entities.add("End User");
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

    private List<String> deriveDataStores(List<String> requirements) {
        String text = String.join(" ", requirements).toLowerCase(Locale.ROOT);
        Set<String> stores = new LinkedHashSet<>();

        stores.add("Requirements Store");
        stores.add("Architecture Store");

        if (text.contains("user") || text.contains("auth") || text.contains("login")) {
            stores.add("Identity Store");
        }
        if (text.contains("audit") || text.contains("compliance")) {
            stores.add("Audit Log Store");
        }
        if (text.contains("event") || text.contains("stream")) {
            stores.add("Event Store");
        }

        return new ArrayList<>(stores);
    }

    private String buildLevel0PlantUml(String architectureStyle, List<String> externalEntities) {
        StringBuilder sb = new StringBuilder();
        sb.append("@startuml\n");
        sb.append("left to right direction\n");
        sb.append("skinparam shadowing false\n");
        sb.append("skinparam packageStyle rectangle\n");

        for (String entity : externalEntities) {
            sb.append("actor \"").append(escapePlant(entity)).append("\" as ").append(alias(entity)).append("\n");
        }

        sb.append("rectangle \"RAAG System (")
                .append(escapePlant(safe(architectureStyle, "Architecture")))
                .append(")\" as CoreSystem\n");

        for (String entity : externalEntities) {
            String entityAlias = alias(entity);
            sb.append(entityAlias).append(" --> CoreSystem : request\n");
            sb.append("CoreSystem --> ").append(entityAlias).append(" : response\n");
        }

        sb.append("@enduml\n");
        return sb.toString();
    }

    private String buildLevel1PlantUml(String style, Graph<String, DefaultEdge> graph, List<String> stores) {
        StringBuilder sb = new StringBuilder();
        sb.append("@startuml\n");
        sb.append("left to right direction\n");
        sb.append("skinparam shadowing false\n");
        sb.append("skinparam componentStyle rectangle\n");

        sb.append("title RAAG DFD Level 1 - ").append(escapePlant(safe(style, "Architecture"))).append("\n");

        List<String> vertices = graph.vertexSet().stream().sorted().toList();
        for (String vertex : vertices) {
            sb.append("component \"").append(escapePlant(vertex)).append("\" as ").append(alias(vertex)).append("\n");
        }

        for (String store : stores) {
            sb.append("database \"").append(escapePlant(store)).append("\" as ").append(alias(store)).append("\n");
        }

        for (DefaultEdge edge : graph.edgeSet()) {
            String source = graph.getEdgeSource(edge);
            String target = graph.getEdgeTarget(edge);
            sb.append(alias(source)).append(" --> ").append(alias(target)).append("\n");
        }

        for (String store : stores) {
            String writer = chooseWriterForStore(store, vertices);
            if (writer != null) {
                sb.append(alias(writer)).append(" --> ").append(alias(store)).append(" : read/write\n");
            }
        }

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
        List<String> componentNames = components.stream().map(Component::getName).toList();
        Map<String, Object> response = llmClient.analyzeTraceability(requirements, componentNames);

        List<Map<String, Object>> matrixRows = llmClient.readListOfMaps(response.get("matrix"));
        List<TraceabilityEntry> matrix = new ArrayList<>();

        for (Map<String, Object> row : matrixRows) {
            String requirement = safe(row.get("requirement"), "");
            if (requirement.isBlank()) {
                continue;
            }
            List<String> mappedComponents = stringList(row.get("components"));
            matrix.add(new TraceabilityEntry(requirement, mappedComponents));
        }

        if (matrix.isEmpty()) {
            return fallbackTraceability(requirements, componentNames);
        }

        List<String> untraced = stringList(response.get("untraced_requirements"));
        List<Map<String, Object>> densityRows = llmClient.readListOfMaps(response.get("high_density_components"));

        List<HighDensityComponent> highDensity = new ArrayList<>();
        for (Map<String, Object> row : densityRows) {
            String component = safe(row.get("component"), "");
            if (component.isBlank()) {
                continue;
            }
            highDensity.add(new HighDensityComponent(component, intValue(row.get("requirement_count"), 0)));
        }

        return new TraceabilityMatrixResult(matrix, untraced, highDensity);
    }

    private TraceabilityMatrixResult fallbackTraceability(List<String> requirements, List<String> componentNames) {
        List<TraceabilityEntry> matrix = new ArrayList<>();
        Map<String, Integer> coverage = new HashMap<>();

        for (String component : componentNames) {
            coverage.put(component, 0);
        }

        for (String requirement : requirements) {
            String lower = requirement.toLowerCase(Locale.ROOT);
            List<String> mapped = componentNames.stream()
                    .filter(component -> {
                        String[] tokens = component.toLowerCase(Locale.ROOT).split("[^a-z0-9]+");
                        for (String token : tokens) {
                            if (token.length() > 2 && lower.contains(token)) {
                                return true;
                            }
                        }
                        return false;
                    })
                    .collect(Collectors.toCollection(ArrayList::new));

            if (mapped.isEmpty() && lower.contains("user")) {
                componentNames.stream()
                        .filter(component -> component.toLowerCase(Locale.ROOT).contains("gateway"))
                        .findFirst()
                        .ifPresent(mapped::add);
            }

            matrix.add(new TraceabilityEntry(requirement, mapped));

            for (String component : mapped) {
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

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok", "service", "architecture");
    }
}
