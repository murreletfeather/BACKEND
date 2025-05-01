package com.tim.qmorph.service;

import com.tim.qmorph.meshing.QMorph;
import com.tim.qmorph.meshing.MeshLoader;
import com.tim.qmorph.geom.Node;
import com.tim.qmorph.geom.Element;
import com.tim.qmorph.geom.Edge;
import com.tim.qmorph.geom.Triangle;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.io.BufferedReader;
import java.io.StringReader;

@Service
public class MeshProcessingService {
    private static final Logger logger = LoggerFactory.getLogger(MeshProcessingService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String processMesh(String meshData, String fileType) throws Exception {
        try {
            QMorph qmorph = new QMorph();

            if ("mesh".equals(fileType)) {
                // Process mesh file format
                List<double[]> triangleCoordinates = new ArrayList<>();
                BufferedReader reader = new BufferedReader(new StringReader(meshData));
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty())
                        continue;

                    // Split by comma and remove any whitespace
                    String[] parts = line.split(",");
                    if (parts.length == 6) {
                        double[] triangle = new double[6];
                        for (int i = 0; i < 6; i++) {
                            // Remove any whitespace and parse the number
                            triangle[i] = Double.parseDouble(parts[i].trim());
                        }
                        triangleCoordinates.add(triangle);
                    } else {
                        logger.warn("Skipping invalid line: {}", line);
                    }
                }

                if (triangleCoordinates.isEmpty()) {
                    throw new IllegalArgumentException("No valid triangle coordinates found in input data");
                }

                logger.debug("Found {} triangles in input data", triangleCoordinates.size());

                double[][] triangles = triangleCoordinates.toArray(new double[0][]);
                List<Triangle> triangleList = MeshLoader.loadTriangleMeshFromArray(triangles, true, true);

                if (triangleList == null || triangleList.isEmpty()) {
                    throw new IllegalArgumentException("Failed to create triangle mesh from input data");
                }

                qmorph.triangleList.addAll(triangleList);
                qmorph.nodeList.addAll(MeshLoader.nodeList);
                qmorph.edgeList.addAll(MeshLoader.edgeList);

            } else {
                // Process JSON format
                Map<String, Object> meshJson = objectMapper.readValue(meshData, Map.class);
                List<Node> nodes = convertToNodes(meshJson);
                List<Element> elements = convertToElements(meshJson, nodes);

                if (nodes.isEmpty()) {
                    throw new IllegalArgumentException("No nodes found in input data");
                }

                qmorph.nodeList.addAll(nodes);
                for (Element element : elements) {
                    if (element instanceof Triangle) {
                        qmorph.triangleList.add((Triangle) element);
                    }
                }
            }

            // Process the mesh
            logger.debug("Initializing QMorph");
            qmorph.init();
            logger.debug("Running QMorph");
            qmorph.run();

            // Get the processed results
            List<Node> processedNodes = qmorph.nodeList;
            List<Element> processedElements = qmorph.elementList;

            // Convert back to JSON
            Map<String, Object> result = new HashMap<>();
            result.put("nodes", convertNodesToJson(processedNodes));
            result.put("edges", convertElementsToJson(processedElements));

            String jsonResult = objectMapper.writeValueAsString(result);
            logger.debug("Processing complete. Result: {}", jsonResult);
            return jsonResult;
        } catch (Exception e) {
            logger.error("Error processing mesh", e);
            throw new Exception("Error processing mesh: " + e.getMessage());
        }
    }

    private List<Node> convertToNodes(Map<String, Object> meshJson) {
        List<Node> nodes = new ArrayList<>();
        List<Map<String, Object>> nodesData = (List<Map<String, Object>>) meshJson.get("nodes");

        if (nodesData != null) {
            for (Map<String, Object> nodeData : nodesData) {
                double x = ((Number) nodeData.get("x")).doubleValue();
                double y = ((Number) nodeData.get("y")).doubleValue();
                nodes.add(new Node(x, y));
            }
        }

        return nodes;
    }

    private List<Element> convertToElements(Map<String, Object> meshJson, List<Node> existingNodes) {
        List<Element> elements = new ArrayList<>();
        List<Map<String, Object>> edgesData = (List<Map<String, Object>>) meshJson.get("edges");

        if (edgesData != null) {
            Map<String, Node> nodeMap = new HashMap<>();
            // Create a map of node coordinates to Node objects for quick lookup
            for (Node node : existingNodes) {
                nodeMap.put(String.format("%.6f,%.6f", node.x, node.y), node);
            }

            List<Edge> edges = new ArrayList<>();
            for (Map<String, Object> edgeData : edgesData) {
                Map<String, Object> source = (Map<String, Object>) edgeData.get("source");
                Map<String, Object> target = (Map<String, Object>) edgeData.get("target");

                double sourceX = ((Number) source.get("x")).doubleValue();
                double sourceY = ((Number) source.get("y")).doubleValue();
                double targetX = ((Number) target.get("x")).doubleValue();
                double targetY = ((Number) target.get("y")).doubleValue();

                // Try to find existing nodes first
                String sourceKey = String.format("%.6f,%.6f", sourceX, sourceY);
                String targetKey = String.format("%.6f,%.6f", targetX, targetY);

                Node sourceNode = nodeMap.get(sourceKey);
                if (sourceNode == null) {
                    sourceNode = new Node(sourceX, sourceY);
                    nodeMap.put(sourceKey, sourceNode);
                }

                Node targetNode = nodeMap.get(targetKey);
                if (targetNode == null) {
                    targetNode = new Node(targetX, targetY);
                    nodeMap.put(targetKey, targetNode);
                }

                Edge edge = new Edge(sourceNode, targetNode);
                edge.connectNodes(); // Connect the edge to its nodes
                edges.add(edge);
            }

            // Create triangles from edges
            // We'll create triangles by finding sets of three connected edges
            for (int i = 0; i < edges.size(); i++) {
                Edge e1 = edges.get(i);
                for (int j = i + 1; j < edges.size(); j++) {
                    Edge e2 = edges.get(j);
                    if (!shareNode(e1, e2))
                        continue;

                    for (int k = j + 1; k < edges.size(); k++) {
                        Edge e3 = edges.get(k);
                        if (formTriangle(e1, e2, e3)) {
                            Triangle triangle = new Triangle(e1, e2, e3);
                            elements.add(triangle);
                            // Connect edges to the triangle
                            e1.connectToElement(triangle);
                            e2.connectToElement(triangle);
                            e3.connectToElement(triangle);
                        }
                    }
                }
            }
        }

        return elements;
    }

    private boolean shareNode(Edge e1, Edge e2) {
        return e1.leftNode.equals(e2.leftNode) ||
                e1.leftNode.equals(e2.rightNode) ||
                e1.rightNode.equals(e2.leftNode) ||
                e1.rightNode.equals(e2.rightNode);
    }

    private boolean formTriangle(Edge e1, Edge e2, Edge e3) {
        // Check if these three edges form a triangle
        Set<Node> nodes = new HashSet<>();
        nodes.add(e1.leftNode);
        nodes.add(e1.rightNode);
        nodes.add(e2.leftNode);
        nodes.add(e2.rightNode);
        nodes.add(e3.leftNode);
        nodes.add(e3.rightNode);

        // A triangle should have exactly 3 unique nodes
        return nodes.size() == 3;
    }

    private List<Map<String, Object>> convertNodesToJson(List<Node> nodes) {
        List<Map<String, Object>> nodesJson = new ArrayList<>();

        for (Node node : nodes) {
            Map<String, Object> nodeData = new HashMap<>();
            nodeData.put("x", node.x);
            nodeData.put("y", node.y);
            nodesJson.add(nodeData);
        }

        return nodesJson;
    }

    private List<Map<String, Object>> convertElementsToJson(List<Element> elements) {
        List<Map<String, Object>> edgesJson = new ArrayList<>();
        Set<String> addedEdges = new HashSet<>(); // To prevent duplicate edges

        for (Element element : elements) {
            // For each edge in the element
            for (Edge edge : element.edgeList) {
                // Create a unique key for this edge
                String edgeKey = String.format("%.6f,%.6f-%.6f,%.6f",
                        edge.leftNode.x, edge.leftNode.y,
                        edge.rightNode.x, edge.rightNode.y);

                // Only add the edge if we haven't seen it before
                if (!addedEdges.contains(edgeKey)) {
                    Map<String, Object> edgeData = new HashMap<>();

                    // Source node
                    Map<String, Object> source = new HashMap<>();
                    source.put("x", edge.leftNode.x);
                    source.put("y", edge.leftNode.y);

                    // Target node
                    Map<String, Object> target = new HashMap<>();
                    target.put("x", edge.rightNode.x);
                    target.put("y", edge.rightNode.y);

                    edgeData.put("source", source);
                    edgeData.put("target", target);

                    edgesJson.add(edgeData);
                    addedEdges.add(edgeKey);
                }
            }
        }

        return edgesJson;
    }
}