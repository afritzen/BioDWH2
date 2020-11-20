package de.unibi.agbi.biodwh2.core.model.graph;

import de.unibi.agbi.biodwh2.core.exceptions.GraphCacheException;
import org.dizitart.no2.*;
import org.dizitart.no2.objects.ObjectFilter;
import org.dizitart.no2.objects.ObjectRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.dizitart.no2.objects.filters.ObjectFilters.*;

public final class Graph {
    public static final String EXTENSION = "db";
    private static final FindOptions LIMIT_ONE_OPTION = FindOptions.limit(0, 1);

    private Nitrite database;
    private ObjectRepository<Node> nodes;
    private ObjectRepository<Edge> edges;
    private final Map<Class<?>, ClassMapping> classMappingsCache = new HashMap<>();
    private final Set<String> userDefinedNodeIndexPropertyKeys = new HashSet<>();

    public Graph(final String databaseFilePath) {
        this(databaseFilePath, false);
    }

    public Graph(final String databaseFilePath, final boolean reopen) {
        if (!reopen)
            deleteOldDatabaseFile(databaseFilePath);
        database = openDatabase(databaseFilePath);
        nodes = database.getRepository(Node.class);
        edges = database.getRepository(Edge.class);
        createInternalIndicesIfNotExist();
    }

    private void deleteOldDatabaseFile(final String filePath) {
        try {
            Files.deleteIfExists(Paths.get(filePath));
        } catch (IOException e) {
            throw new GraphCacheException("Failed to remove old persisted database file '" + filePath + "'", e);
        }
    }

    private static Nitrite openDatabase(final String filePath) {
        return Nitrite.builder().compressed().filePath(filePath).openOrCreate();
    }

    private void createInternalIndicesIfNotExist() {
        addIndexIfNotExists(nodes, Node.LABEL_FIELD);
        addIndexIfNotExists(edges, Edge.FROM_ID_FIELD);
        addIndexIfNotExists(edges, Edge.TO_ID_FIELD);
        addIndexIfNotExists(edges, Edge.LABEL_FIELD);
    }

    private void addIndexIfNotExists(final ObjectRepository<?> repository, final String key) {
        if (!repository.hasIndex(key))
            repository.createIndex(key, IndexOptions.indexOptions(IndexType.NonUnique, true));
    }

    public void setNodeIndexPropertyKeys(final String... keys) {
        userDefinedNodeIndexPropertyKeys.addAll(Arrays.asList(keys));
        for (final String key : keys)
            addIndexIfNotExists(nodes, key);
    }

    public void prefixAllLabels(final String prefix) {
        dropIndexIfExists(nodes, Node.LABEL_FIELD);
        dropIndexIfExists(edges, Edge.LABEL_FIELD);
        for (final Document document : nodes.getDocumentCollection().find()) {
            document.put(Node.LABEL_FIELD, prefix + document.get(Node.LABEL_FIELD));
            nodes.getDocumentCollection().update(document);
        }
        for (final Document document : edges.getDocumentCollection().find()) {
            document.put(Edge.LABEL_FIELD, prefix + document.get(Edge.LABEL_FIELD));
            edges.getDocumentCollection().update(document);
        }
        createInternalIndicesIfNotExist();
    }

    private void dropIndexIfExists(final ObjectRepository<?> repository, final String key) {
        if (repository.hasIndex(key))
            repository.dropIndex(key);
    }

    private void dropAllInternalIndices() {
        dropIndexIfExists(nodes, Node.LABEL_FIELD);
        dropIndexIfExists(edges, Edge.FROM_ID_FIELD);
        dropIndexIfExists(edges, Edge.TO_ID_FIELD);
        dropIndexIfExists(edges, Edge.LABEL_FIELD);
    }

    public Node addNode(final String label) {
        final Node n = new Node(label);
        nodes.insert(n);
        return n;
    }

    public Node addNode(final String label, final String propertyKey, final Object propertyValue) {
        final Node n = new Node(label);
        n.setProperty(propertyKey, propertyValue);
        nodes.insert(n);
        return n;
    }

    public Node addNode(final String label, final String propertyKey1, final Object propertyValue1,
                        final String propertyKey2, final Object propertyValue2) {
        final Node n = new Node(label);
        n.setProperty(propertyKey1, propertyValue1);
        n.setProperty(propertyKey2, propertyValue2);
        nodes.insert(n);
        return n;
    }

    public Node addNode(final String label, final String propertyKey1, final Object propertyValue1,
                        final String propertyKey2, final Object propertyValue2, final String propertyKey3,
                        final Object propertyValue3) {
        final Node n = new Node(label);
        n.setProperty(propertyKey1, propertyValue1);
        n.setProperty(propertyKey2, propertyValue2);
        n.setProperty(propertyKey3, propertyValue3);
        nodes.insert(n);
        return n;
    }

    public Node addNode(final String label, final String propertyKey1, final Object propertyValue1,
                        final String propertyKey2, final Object propertyValue2, final String propertyKey3,
                        final Object propertyValue3, final String propertyKey4, final Object propertyValue4) {
        final Node n = new Node(label);
        n.setProperty(propertyKey1, propertyValue1);
        n.setProperty(propertyKey2, propertyValue2);
        n.setProperty(propertyKey3, propertyValue3);
        n.setProperty(propertyKey4, propertyValue4);
        nodes.insert(n);
        return n;
    }

    public Node addNode(final String label, final Map<String, Object> properties) {
        final Node n = new Node(label);
        for (Map.Entry<String, Object> entry : properties.entrySet())
            n.setProperty(entry.getKey(), entry.getValue());
        nodes.insert(n);
        return n;
    }

    public NodeBuilder buildNode() {
        return new NodeBuilder(this);
    }

    public final <T> Node addNodeFromModel(final T obj) {
        final ClassMapping mapping = getClassMappingFromCache(obj.getClass());
        final Node node = new Node(mapping.label);
        mapping.setNodeProperties(node, obj);
        nodes.insert(node);
        return node;
    }

    private ClassMapping getClassMappingFromCache(final Class<?> type) {
        if (!classMappingsCache.containsKey(type))
            classMappingsCache.put(type, new ClassMapping(type));
        return classMappingsCache.get(type);
    }

    public void update(final Node node) {
        if (node == null)
            throw new GraphCacheException("Failed to update node because it is null");
        nodes.update(node.getEqFilter(), node, false);
    }

    public Edge addEdge(final Node from, final Node to, final String label) {
        validateSourceNode(from);
        validateTargetNode(to);
        return addEdge(from.getId(), to.getId(), label);
    }

    private void validateSourceNode(final Node node) {
        if (node == null)
            throw new GraphCacheException("Failed to add edge because the source node is null");
    }

    private void validateTargetNode(final Node node) {
        if (node == null)
            throw new GraphCacheException("Failed to add edge because the target node is null");
    }

    public Edge addEdge(final long fromId, final Node to, final String label) {
        validateTargetNode(to);
        return addEdge(fromId, to.getId(), label);
    }

    public Edge addEdge(final Node from, final long toId, final String label) {
        validateSourceNode(from);
        return addEdge(from.getId(), toId, label);
    }

    public Edge addEdge(final long fromId, final long toId, final String label) {
        validateEdgeLabel(label);
        final Edge e = new Edge(fromId, toId, label);
        edges.insert(e);
        return e;
    }

    private void validateEdgeLabel(final String label) {
        if (label == null || label.length() == 0)
            throw new GraphCacheException("Failed to add edge because the label is null or empty");
    }

    public Edge addEdge(final Node from, final Node to, final String label, final String propertyKey,
                        final Object propertyValue) {
        validateSourceNode(from);
        validateTargetNode(to);
        return addEdge(from.getId(), to.getId(), label, propertyKey, propertyValue);
    }

    public Edge addEdge(final long fromId, final Node to, final String label, final String propertyKey,
                        final Object propertyValue) {
        validateTargetNode(to);
        return addEdge(fromId, to.getId(), label, propertyKey, propertyValue);
    }

    public Edge addEdge(final Node from, final long toId, final String label, final String propertyKey,
                        final Object propertyValue) {
        validateSourceNode(from);
        return addEdge(from.getId(), toId, label, propertyKey, propertyValue);
    }

    public Edge addEdge(final long fromId, final long toId, final String label, final String propertyKey,
                        final Object propertyValue) {
        validateEdgeLabel(label);
        final Edge e = new Edge(fromId, toId, label);
        e.setProperty(propertyKey, propertyValue);
        edges.insert(e);
        return e;
    }

    public Edge addEdge(final Node from, final Node to, final String label, final String propertyKey1,
                        final Object propertyValue1, final String propertyKey2, final Object propertyValue2) {
        validateSourceNode(from);
        validateTargetNode(to);
        return addEdge(from.getId(), to.getId(), label, propertyKey1, propertyValue1, propertyKey2, propertyValue2);
    }

    public Edge addEdge(final long fromId, final Node to, final String label, final String propertyKey1,
                        final Object propertyValue1, final String propertyKey2, final Object propertyValue2) {
        validateTargetNode(to);
        return addEdge(fromId, to.getId(), label, propertyKey1, propertyValue1, propertyKey2, propertyValue2);
    }

    public Edge addEdge(final Node from, final long toId, final String label, final String propertyKey1,
                        final Object propertyValue1, final String propertyKey2, final Object propertyValue2) {
        validateSourceNode(from);
        return addEdge(from.getId(), toId, label, propertyKey1, propertyValue1, propertyKey2, propertyValue2);
    }

    public Edge addEdge(final long fromId, final long toId, final String label, final String propertyKey1,
                        final Object propertyValue1, final String propertyKey2, final Object propertyValue2) {
        validateEdgeLabel(label);
        final Edge e = new Edge(fromId, toId, label);
        e.setProperty(propertyKey1, propertyValue1);
        e.setProperty(propertyKey2, propertyValue2);
        edges.insert(e);
        return e;
    }

    public Edge addEdge(final Node from, final Node to, final String label, final Map<String, Object> properties) {
        validateSourceNode(from);
        validateTargetNode(to);
        return addEdge(from.getId(), to.getId(), label, properties);
    }

    public Edge addEdge(final long fromId, final Node to, final String label, final Map<String, Object> properties) {
        validateTargetNode(to);
        return addEdge(fromId, to.getId(), label, properties);
    }

    public Edge addEdge(final Node from, final long toId, final String label, final Map<String, Object> properties) {
        validateSourceNode(from);
        return addEdge(from.getId(), toId, label, properties);
    }

    public Edge addEdge(final long fromId, final long toId, final String label, final Map<String, Object> properties) {
        validateEdgeLabel(label);
        final Edge e = new Edge(fromId, toId, label);
        for (Map.Entry<String, Object> entry : properties.entrySet())
            e.setProperty(entry.getKey(), entry.getValue());
        edges.insert(e);
        return e;
    }

    public EdgeBuilder buildEdge() {
        return new EdgeBuilder(this);
    }

    public void update(final Edge edge) {
        if (edge == null)
            throw new GraphCacheException("Failed to update edge because it is null");
        edges.update(edge.getEqFilter(), edge, false);
    }

    public Iterable<Node> getNodes() {
        return () -> nodes.find().iterator();
    }

    public Iterable<Node> getNodes(final String label) {
        if (label == null || label.length() == 0)
            return getNodes();
        return () -> nodes.find(eq(Node.LABEL_FIELD, label)).iterator();
    }

    public Iterable<Edge> getEdges() {
        return () -> edges.find().iterator();
    }

    public Iterable<Edge> getEdges(final String label) {
        if (label == null || label.length() == 0)
            return getEdges();
        return () -> edges.find(eq(Edge.LABEL_FIELD, label)).iterator();
    }

    public long getNumberOfNodes() {
        return nodes.size();
    }

    public long getNumberOfEdges() {
        return edges.size();
    }

    public Node getNode(final long nodeId) {
        return nodes.getById(NitriteId.createId(nodeId));
    }

    public Edge getEdge(final long edgeId) {
        return edges.getById(NitriteId.createId(edgeId));
    }

    public Node findNode(final String label) {
        return nodes.find(eq(Node.LABEL_FIELD, label), LIMIT_ONE_OPTION).firstOrDefault();
    }

    public Node findNode(final String label, final String propertyKey, final Object value) {
        return nodes.find(and(eq(Node.LABEL_FIELD, label), eq(propertyKey, value)), LIMIT_ONE_OPTION).firstOrDefault();
    }

    public Node findNode(final String label, final String propertyKey1, final Object value1, final String propertyKey2,
                         final Object value2) {
        return nodes.find(and(eq(Node.LABEL_FIELD, label), eq(propertyKey1, value1), eq(propertyKey2, value2)),
                          LIMIT_ONE_OPTION).firstOrDefault();
    }

    public Node findNode(final String label, final String propertyKey1, final Object value1, final String propertyKey2,
                         final Object value2, final String propertyKey3, final Object value3) {
        return nodes.find(and(eq(Node.LABEL_FIELD, label), eq(propertyKey1, value1), eq(propertyKey2, value2),
                              eq(propertyKey3, value3)), LIMIT_ONE_OPTION).firstOrDefault();
    }

    public Node findNode(final String label, final Map<String, Object> properties) {
        final ObjectFilter[] filter = new ObjectFilter[properties.size() + 1];
        filter[0] = eq(Node.LABEL_FIELD, label);
        int index = 1;
        for (final String propertyKey : properties.keySet())
            filter[index++] = eq(propertyKey, properties.get(propertyKey));
        return nodes.find(and(filter), LIMIT_ONE_OPTION).firstOrDefault();
    }

    public Node findNode(final String propertyKey, final Object value) {
        return nodes.find(eq(propertyKey, value), LIMIT_ONE_OPTION).firstOrDefault();
    }

    public Node findNode(final String propertyKey1, final Object value1, final String propertyKey2,
                         final Object value2) {
        return nodes.find(and(eq(propertyKey1, value1), eq(propertyKey2, value2)), LIMIT_ONE_OPTION).firstOrDefault();
    }

    public Node findNode(final String propertyKey1, final Object value1, final String propertyKey2, final Object value2,
                         final String propertyKey3, final Object value3) {
        return nodes.find(and(eq(propertyKey1, value1), eq(propertyKey2, value2), eq(propertyKey3, value3)),
                          LIMIT_ONE_OPTION).firstOrDefault();
    }

    public Node findNode(final Map<String, Object> properties) {
        if (properties.size() == 0)
            return null;
        final ObjectFilter[] filter = new ObjectFilter[properties.size()];
        int index = 0;
        for (final String propertyKey : properties.keySet())
            filter[index++] = eq(propertyKey, properties.get(propertyKey));
        return nodes.find(and(filter), LIMIT_ONE_OPTION).firstOrDefault();
    }

    public Iterable<Node> findNodes(final String label) {
        return () -> nodes.find(eq(Node.LABEL_FIELD, label)).iterator();
    }

    public Iterable<Node> findNodes(final String label, final String propertyKey, final Object value) {
        return () -> nodes.find(and(eq(Node.LABEL_FIELD, label), eq(propertyKey, value))).iterator();
    }

    public Iterable<Node> findNodes(final String label, final String propertyKey1, final Object value1,
                                    final String propertyKey2, final Object value2) {
        return () -> nodes.find(and(eq(Node.LABEL_FIELD, label), eq(propertyKey1, value1), eq(propertyKey2, value2)))
                          .iterator();
    }

    public Iterable<Node> findNodes(final String label, final String propertyKey1, final Object value1,
                                    final String propertyKey2, final Object value2, final String propertyKey3,
                                    final Object value3) {
        return () -> nodes.find(and(eq(Node.LABEL_FIELD, label), eq(propertyKey1, value1), eq(propertyKey2, value2),
                                    eq(propertyKey3, value3))).iterator();
    }

    public Iterable<Node> findNodes(final String label, final Map<String, Object> properties) {
        final ObjectFilter[] filter = new ObjectFilter[properties.size() + 1];
        filter[0] = eq(Node.LABEL_FIELD, label);
        int index = 1;
        for (final String propertyKey : properties.keySet())
            filter[index++] = eq(propertyKey, properties.get(propertyKey));
        return () -> nodes.find(and(filter)).iterator();
    }

    public Iterable<Node> findNodes(final String propertyKey, final Object value) {
        return () -> nodes.find(eq(propertyKey, value)).iterator();
    }

    public Iterable<Node> findNodes(final String propertyKey1, final Object value1, final String propertyKey2,
                                    final Object value2) {
        return () -> nodes.find(and(eq(propertyKey1, value1), eq(propertyKey2, value2))).iterator();
    }

    public Iterable<Node> findNodes(final String propertyKey1, final Object value1, final String propertyKey2,
                                    final Object value2, final String propertyKey3, final Object value3) {
        return () -> nodes.find(and(eq(propertyKey1, value1), eq(propertyKey2, value2), eq(propertyKey3, value3)))
                          .iterator();
    }

    public Iterable<Node> findNodes(final Map<String, Object> properties) {
        if (properties.size() == 0)
            return () -> nodes.find().iterator();
        final ObjectFilter[] filter = new ObjectFilter[properties.size()];
        int index = 0;
        for (final String propertyKey : properties.keySet())
            filter[index++] = eq(propertyKey, properties.get(propertyKey));
        return () -> nodes.find(and(filter)).iterator();
    }

    public Edge findEdge(final String label) {
        return edges.find(eq(Edge.LABEL_FIELD, label), LIMIT_ONE_OPTION).firstOrDefault();
    }

    public Edge findEdge(final String label, final String propertyKey, final Object value) {
        return edges.find(and(eq(Edge.LABEL_FIELD, label), eq(propertyKey, value)), LIMIT_ONE_OPTION).firstOrDefault();
    }

    public Edge findEdge(final String label, final String propertyKey1, final Object value1, final String propertyKey2,
                         final Object value2) {
        return edges.find(and(eq(Edge.LABEL_FIELD, label), eq(propertyKey1, value1), eq(propertyKey2, value2)),
                          LIMIT_ONE_OPTION).firstOrDefault();
    }

    public Edge findEdge(final String label, final String propertyKey1, final Object value1, final String propertyKey2,
                         final Object value2, final String propertyKey3, final Object value3) {
        return edges.find(and(eq(Edge.LABEL_FIELD, label), eq(propertyKey1, value1), eq(propertyKey2, value2),
                              eq(propertyKey3, value3)), LIMIT_ONE_OPTION).firstOrDefault();
    }

    public Edge findEdge(final String label, final Map<String, Object> properties) {
        final ObjectFilter[] filter = new ObjectFilter[properties.size() + 1];
        filter[0] = eq(Edge.LABEL_FIELD, label);
        int index = 1;
        for (final String propertyKey : properties.keySet())
            filter[index++] = eq(propertyKey, properties.get(propertyKey));
        return edges.find(and(filter), LIMIT_ONE_OPTION).firstOrDefault();
    }

    public Iterable<Edge> findEdges(final String label) {
        return () -> edges.find(eq(Edge.LABEL_FIELD, label)).iterator();
    }

    public Iterable<Edge> findEdges(final String label, final String propertyKey, final Object value) {
        return () -> edges.find(and(eq(Edge.LABEL_FIELD, label), eq(propertyKey, value))).iterator();
    }

    public Iterable<Edge> findEdges(final String label, final String propertyKey1, final Object value1,
                                    final String propertyKey2, final Object value2) {
        return () -> edges.find(and(eq(Edge.LABEL_FIELD, label), eq(propertyKey1, value1), eq(propertyKey2, value2)))
                          .iterator();
    }

    public Iterable<Edge> findEdges(final String label, final String propertyKey1, final Object value1,
                                    final String propertyKey2, final Object value2, final String propertyKey3,
                                    final Object value3) {
        return () -> edges.find(and(eq(Edge.LABEL_FIELD, label), eq(propertyKey1, value1), eq(propertyKey2, value2),
                                    eq(propertyKey3, value3))).iterator();
    }

    public Iterable<Edge> findEdges(final String label, final Map<String, Object> properties) {
        final ObjectFilter[] filter = new ObjectFilter[properties.size() + 1];
        filter[0] = eq(Edge.LABEL_FIELD, label);
        int index = 1;
        for (final String propertyKey : properties.keySet())
            filter[index++] = eq(propertyKey, properties.get(propertyKey));
        return () -> edges.find(and(filter)).iterator();
    }

    public Long[] getAdjacentNodeIdsForEdgeLabel(final long nodeId, final String edgeLabel) {
        final Set<Long> nodeIds = new HashSet<>();
        final ObjectFilter filter = and(eq(Edge.LABEL_FIELD, edgeLabel),
                                        or(eq(Edge.FROM_ID_FIELD, nodeId), eq(Edge.TO_ID_FIELD, nodeId)));
        for (final Edge edge : edges.find(filter)) {
            nodeIds.add(edge.getFromId());
            nodeIds.add(edge.getToId());
        }
        nodeIds.remove(nodeId);
        return nodeIds.toArray(new Long[0]);
    }

    public void mergeNodes(final Node first, final Node second) {
        for (final Edge edge : edges.find(eq(Edge.FROM_ID_FIELD, second.getId()))) {
            edge.setFromId(first.getId());
            update(edge);
        }
        for (final Edge edge : edges.find(eq(Edge.TO_ID_FIELD, second.getId()))) {
            edge.setToId(first.getId());
            update(edge);
        }
        // TODO: properties
        nodes.remove(second);
    }

    public void mergeDatabase(final String filePath) {
        final Graph databaseToMerge = new Graph(filePath, true);
        for (final Index index : databaseToMerge.nodes.listIndices())
            if (!nodes.hasIndex(index.getField()))
                nodes.createIndex(index.getField(), IndexOptions.indexOptions(index.getIndexType(), true));
        for (final Index index : databaseToMerge.edges.listIndices())
            if (!edges.hasIndex(index.getField()))
                edges.createIndex(index.getField(), IndexOptions.indexOptions(index.getIndexType(), true));
        final Map<Long, Long> mapping = new HashMap<>();
        for (final Node n : databaseToMerge.nodes.find()) {
            final Long oldId = n.getId();
            n.resetId();
            nodes.insert(n);
            mapping.put(oldId, n.getId());
        }
        for (final Edge e : databaseToMerge.edges.find()) {
            e.resetId();
            e.setProperty(Edge.FROM_ID_FIELD, mapping.get(e.getFromId()));
            e.setProperty(Edge.TO_ID_FIELD, mapping.get(e.getToId()));
            edges.insert(e);
        }
        databaseToMerge.dispose();
    }

    public void dispose() {
        if (database != null && !database.isClosed())
            database.close();
        nodes = null;
        edges = null;
        database = null;
    }

    public static Graph createTempGraph() throws IOException {
        final Path tempFilePath = Files.createTempFile("graphdb_test", ".db");
        return new Graph(tempFilePath.toString());
    }
}
