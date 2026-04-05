# Distributed Cache System

A Java-based distributed cache system with advanced features including LRU eviction, prefetching, and pluggable distribution strategies.

## Table of Contents

- [Architecture Overview](#architecture-overview)
- [Class Documentation](#class-documentation)
  - [Client (Entry Point)](#client-java---entry-point)
  - [ApiServerGateway](#apiservergatewayjava---gateway)
  - [Cluster](#clusterjava---node-manager)
  - [Node (Cache)](#nodejava---cache-unit)
  - [Repository](#repositoryjava---data-access)
  - [Prefetch Interface](#prefetchjava---prefetch-interface)
  - [FixedFetch](#fixedfetchjava---prefetch-implementation)
  - [EvictionStrategy](#evictionstrategyjava---eviction-interface)
  - [LruEvictionStrategy](#lruevictionstrategyjava---lru-implementation)
  - [DataDistribution](#datadistributionjava---distribution-base)
  - [CopyDataDistribution](#copydatadistributionjava---distribution-implementation)
  - [INodeSelectionStrategy](#inodeselectionstrategyjava---selection-interface)
  - [ModuloNodeSelectionStrategy](#modulonodeselectionstrategyjava---selection-implementation)
- [Design & Implementation Details](#design--implementation-details)
  - [Why LinkedHashMap for LRU?](#why-linkedhashmap-for-lru)
  - [Eviction Strategy Pattern](#eviction-strategy-pattern)
  - [Prefetch Service](#prefetch-service)
  - [Repository Pattern](#repository-pattern)
  - [Data Distribution](#data-distribution)
- [Project Setup Guide](#project-setup-guide)
- [Running the Application](#running-the-application)
- [Future Enhancements](#future-enhancements)

---

## Architecture Overview

```
                    +------------------+
                    |    Client        |
                    +--------+---------+
                             |
                             v
                    +------------------+
                    |ApiServerGateway  |
                    +--------+---------+
                             |
                             v
                    +------------------+
                    |     Cluster      |
                    +--------+---------+
                             |
              +--------------+-------------+
              |                              |
              v                              v
        +----------+                  +----------+
        |  Node 0  |                  |  Node 1  |
        +----------+                  +----------+
        | Cache    |                  | Cache    |
        | LRU      |                  | LRU      |
        +----------+                  +----------+
              |                              |
              +--------------+---------------+
                             |
                             v
                    +------------------+
                    |   Repository     |
                    |   (Singleton)    |
                    +--------+---------+
                             |
                             v
                    +------------------+
                    |   Fake DB        |
                    | (JSON file)      |
                    +------------------+
```

---

## Class Documentation

### client.java - Entry Point

**Purpose:**
The main entry point of the application. Demonstrates how to use the distributed cache system by creating a cluster, adding nodes, and making get requests.

**Attributes:**
| Attribute | Type | Description |
|-----------|------|-------------|
| (none) | - | This is a static main class with no instance attributes |

**Methods:**
| Method | Description |
|--------|-------------|
| `main(String[] args)` | Creates a node selection strategy, initializes a cluster with nodes, creates an ApiServerGateway, and demonstrates get operations on keys 4 and 3 |

**Relationships:**
- Creates `ModuloNodeSelectionStrategy`
- Creates `Cluster` using the strategy
- Creates `Node` instances (0 and 1)
- Creates `ApiServerGateway` with the cluster
- Calls `ApiServerGateway.get()` to retrieve values

---

### ApiServerGateway.java - Gateway

**Purpose:**
Acts as the API layer that routes client requests to the appropriate cache node based on the node selection strategy. It is the single entry point for all cache get operations.

**Attributes:**
| Attribute | Type | Description |
|-----------|------|-------------|
| cluster | Cluster | Reference to the cluster that manages all cache nodes |

**Methods:**
| Method | Description |
|--------|-------------|
| `ApiServerGateway(Cluster cluster)` | Constructor that initializes the gateway with a cluster reference |
| `get(int key)` | Routes the key to the appropriate node using the node selection strategy and returns the value |

**Relationships:**
- Depends on `Cluster` (composed)
- Uses `INodeSelectionStrategy` via Cluster
- Returns value from `Node.getValue()`

---

### Cluster.java - Node Manager

**Purpose:**
Manages the collection of cache nodes in the distributed system. Handles node addition and removal, and coordinates data redistribution when the cluster topology changes.

**Attributes:**
| Attribute | Type | Description |
|-----------|------|-------------|
| nodesMap | HashMap<Integer, Node> | Map storing node ID to Node instance |
| dataDistribution | DataDistribution | Strategy for redistributing data on node changes |
| nodeSelectionStrategy | INodeSelectionStrategy | Strategy for selecting which node handles a given key |

**Methods:**
| Method | Description |
|--------|-------------|
| `Cluster(INodeSelectionStrategy nodeSelectionStrategy)` | Constructor that initializes the cluster with a node selection strategy |
| `geDataDistribution()` | Lazy initialization of the data distribution strategy (returns CopyDataDistribution) |
| `getNode(int nodeId)` | Retrieves a node by its ID |
| `addNode(Node newNode)` | Adds a new node to the cluster and triggers data redistribution |
| `removeNode(int nodeId)` | Removes a node from the cluster and triggers data redistribution |
| `getNodeSelectionStrategy()` | Returns the node selection strategy |
| `getNodeCount()` | Returns the current number of nodes in the cluster |

**Relationships:**
- Contains multiple `Node` instances
- Uses `DataDistribution` (composition) for redistribution
- Uses `INodeSelectionStrategy` for routing
- Notifies nodes when topology changes via DataDistribution

---

### Node.java - Cache Unit

**Purpose:**
Represents a single cache node in the distributed system. Each node maintains its own in-memory cache with LRU eviction, and can prefetch related keys. This is where the core caching logic resides.

**Attributes:**
| Attribute | Type | Description |
|-----------|------|-------------|
| cache | Map<Integer, String> | The LRU cache storing key-value pairs (LinkedHashMap with access order) |
| id | Integer | Unique identifier for this node |
| repository | Repository | Singleton reference to access the underlying data store |
| prefetch | Prefetch | Prefetch strategy for loading related keys |
| evictionStrategy | EvictionStrategy | Pluggable eviction policy (LRU by default) |
| DEFAULT_THRESHOLD | static final int | Default prefetch threshold = 2 |
| DEFAULT_CAPACITY | static final int | Default cache capacity = 100 |

**Methods:**
| Method | Description |
|--------|-------------|
| `Node(Integer id)` | Constructor with default capacity (100) |
| `Node(Integer id, int capacity)` | Constructor with custom capacity |
| `getValue(int key)` | Returns the value for the key. If not in cache, fetches from repository, caches it, and triggers prefetch |
| `putIntoCache(int key, String value)` | Puts a value into the cache using the eviction strategy |
| `getId()` | Returns the node's ID |
| `getCache()` | Returns the cache map (for redistribution) |

**Relationships:**
- Uses `Repository` (singleton) to fetch data from database
- Uses `Prefetch` (singleton) to prefetch related keys
- Uses `EvictionStrategy` for cache eviction
- Called by `ApiServerGateway` to retrieve values
- Called by `Cluster` via DataDistribution for data redistribution
- Linked with `LinkedHashMap` for LRU behavior

---

### Repository.java - Data Access

**Purpose:**
Acts as the bridge between the cache system and the underlying data store (fake database). Implements the Repository pattern to provide a clean abstraction for data access. Uses singleton pattern as it's stateless.

**Attributes:**
| Attribute | Type | Description |
|-----------|------|-------------|
| instance | static Repository | Singleton instance |
| database | Map<Integer, String> | In-memory map storing the database content |

**Methods:**
| Method | Description |
|--------|-------------|
| `Repository()` | Private constructor that initializes the database and loads data from JSON |
| `getInstance()` | Static synchronized method to get the singleton instance (lazy initialization) |
| `loadDatabase()` | Private method that parses fake-db.json and populates the database map |
| `get(Integer key)` | Returns the value for the given key |
| `containsKey(Integer key)` | Checks if a key exists in the database |

**Relationships:**
- Reads from `fake-db.json` (JSON file)
- Used by `Node` to fetch values not in cache
- Used by `FixedFetch` to prefetch related keys

---

### Prefetch.java - Prefetch Interface

**Purpose:**
Defines the contract for prefetch strategies. Allows pluggable prefetch implementations that can load related keys into the cache proactively.

**Methods:**
| Method | Description |
|--------|-------------|
| `prefetch(Node node, int key)` | Prefetches related keys into the given node's cache |

**Relationships:**
- Implemented by `FixedFetch`
- Used by `Node` to prefetch keys after a cache miss

---

### FixedFetch.java - Prefetch Implementation

**Purpose:**
A prefetch implementation that loads keys within a fixed range around the requested key. Uses singleton pattern as it's stateless and provides consistent behavior across all nodes.

**Attributes:**
| Attribute | Type | Description |
|-----------|------|-------------|
| instance | static FixedFetch | Singleton instance |
| threshold | int | The range size for prefetching (key-threshold to key+threshold) |
| repository | Repository | Reference to the repository for fetching values |

**Methods:**
| Method | Description |
|--------|-------------|
| `FixedFetch(int threshold)` | Private constructor that initializes threshold and repository |
| `getInstance(int threshold)` | Static synchronized method to get the singleton instance |
| `prefetch(Node node, int key)` | Fetches all keys in range [key-threshold, key+threshold] except the key itself, and adds them to the node's cache |

**Relationships:**
- Implements `Prefetch` interface
- Uses `Repository` to fetch prefetch values
- Calls `Node.putIntoCache()` to add prefetched values
- Singleton used by all `Node` instances

---

### EvictionStrategy.java - Eviction Interface

**Purpose:**
Defines the contract for cache eviction strategies using the Strategy Pattern. Allows different eviction policies (LRU, LFU, TTL, etc.) to be plugged in.

**Methods:**
| Method | Description |
|--------|-------------|
| `onAccess(Map<Integer, String> cache, Integer key)` | Called when a key is accessed - allows updating access metadata |
| `onPut(Map<Integer, String> cache, Integer key, String value)` | Called when a key is inserted/updated - handles eviction if needed |

**Relationships:**
- Implemented by `LruEvictionStrategy`
- Used by `Node` to manage cache eviction

---

### LruEvictionStrategy.java - LRU Implementation

**Purpose:**
Implements the Least Recently Used (LRU) eviction strategy. Works in conjunction with LinkedHashMap's access-order mode to track and evict the least recently accessed entries when the cache reaches capacity.

**Attributes:**
| Attribute | Type | Description |
|-----------|------|-------------|
| capacity | int | Maximum number of entries the cache can hold |

**Methods:**
| Method | Description |
|--------|-------------|
| `LruEvictionStrategy(int capacity)` | Constructor that sets the eviction capacity |
| `onAccess(Map<Integer, String> cache, Integer key)` | Moves the accessed key to the end (most recently used position) |
| `onPut(Map<Integer, String> cache, Integer key, String value)` | Handles insertion/update: updates existing keys, removes LRU entry if cache is full before inserting new entry |
| `getCapacity()` | Returns the capacity |

**Relationships:**
- Implements `EvictionStrategy` interface
- Works with `LinkedHashMap` (access-order mode) for efficient LRU tracking
- Used by `Node` for cache management

---

### DataDistribution.java - Distribution Base

**Purpose:**
Abstract base class for data distribution strategies. Defines the contract for redistributing cache data when nodes are added to or removed from the cluster.

**Attributes:**
| Attribute | Type | Description |
|-----------|------|-------------|
| nodeSelectionStrategy | INodeSelectionStrategy | Strategy for determining which node owns a key |
| nodesMap | HashMap<Integer, Node> | Map of all nodes in the cluster |

**Methods:**
| Method | Description |
|--------|-------------|
| `DataDistribution(INodeSelectionStrategy nodeSelectionStrategy, HashMap<Integer, Node> nodesMap)` | Constructor that initializes the strategy and nodes map |
| `additionRearrange(Integer integer)` | Abstract method - called when a node is added |
| `removalRearrange(Node oldNode)` | Abstract method - called when a node is removed |

**Relationships:**
- Extended by `CopyDataDistribution`
- Uses `INodeSelectionStrategy` for key redistribution decisions
- Accesses `Node` instances for cache manipulation

---

### CopyDataDistribution.java - Distribution Implementation

**Purpose:**
Implements data distribution by copying relevant keys to the appropriate nodes when the cluster topology changes. When a node is added, it copies relevant keys from existing nodes. When a node is removed, it redistributes its keys to remaining nodes.

**Attributes:**
(Inherits from DataDistribution)

**Methods:**
| Method | Description |
|--------|-------------|
| `CopyDataDistribution(INodeSelectionStrategy nodeSelectionStrategy, HashMap<Integer, Node> nodesMap)` | Constructor that calls parent constructor |
| `additionRearrange(Integer newNodeId)` | Iterates through all nodes' caches and copies keys that belong to the new node based on the selection strategy |
| `removalRearrange(Node oldNode)` | Redistributes keys from the removed node to remaining nodes based on the selection strategy |

**Relationships:**
- Extends `DataDistribution`
- Uses `Node.getCache()` to read cache contents
- Uses `Node.putIntoCache()` to write cache entries

---

### INodeSelectionStrategy.java - Selection Interface

**Purpose:**
Defines the contract for node selection strategies. Determines which node should handle a given key based on the key value and total number of nodes.

**Methods:**
| Method | Description |
|--------|-------------|
| `getNodeID(int key, int nodeCount)` | Returns the node ID that should handle the given key |

**Relationships:**
- Implemented by `ModuloNodeSelectionStrategy`
- Used by `Cluster` and `ApiServerGateway` to route requests
- Used by `CopyDataDistribution` to determine key ownership

---

### ModuloNodeSelectionStrategy.java - Selection Implementation

**Purpose:**
A simple node selection strategy that uses modulo arithmetic to distribute keys across nodes. Key is assigned to node: `key % nodeCount`.

**Methods:**
| Method | Description |
|--------|-------------|
| `getNodeID(int key, int nodeCount)` | Returns `key % nodeCount` as the target node ID |

**Relationships:**
- Implements `INodeSelectionStrategy`
- Used by `Cluster` for node selection

---

## Design & Implementation Details

### Why LinkedHashMap for LRU?

We chose `LinkedHashMap` with access-order mode as the underlying data structure for the LRU cache. Here's why:

#### 1. **Access-Order Mode**

```java
this.cache = new LinkedHashMap<Integer, String>(capacity, 0.75f, true) {
    @Override
    protected boolean removeEldestEntry(Map.Entry<Integer, String> eldest) {
        return size() > capacity;
    }
};
```

The third parameter `true` enables **access-order** mode. In this mode:
- The iteration order reflects the order of **access** (most recently accessed elements are moved to the end)
- When you call `get(key)` or `put(key, value)`, the accessed/inserted entry moves to the end
- The first element in the iteration order is always the **least recently used (LRU)** element

#### 2. **Automatic Eviction via removeEldestEntry()**

`LinkedHashMap` provides a special method `removeEldestEntry()` that we override. This method is called after each `put()` operation:

- When the map size exceeds the capacity, `removeEldestEntry()` returns `true`
- `LinkedHashMap` automatically removes the eldest (first) entry before adding the new one
- This gives us **O(1)** eviction without any custom tracking logic

#### 3. **Key Behaviors Achieved**

| Behavior | How It's Achieved |
|----------|-------------------|
| **LRU Ordering** | Access-order mode automatically reorders on get/put operations |
| **O(1) Access** | `LinkedHashMap` provides O(1) get/put operations |
| **Automatic Eviction** | `removeEldestEntry()` triggers when size > capacity |
| **Insertion Order** | For new entries, they're added at the end (most recently used position) |
| **Access Tracking** | Every access moves the element to end - no manual timestamp tracking needed |

#### 4. **Alternative Approaches Considered**

- **HashMap + Manual LinkedList**: Would require manual synchronization between two data structures
- **Timestamp-based HashMap**: Would require O(n) scanning to find the oldest entry
- **Custom LRU implementation**: Would require significant code and potential for bugs

The `LinkedHashMap` approach gives us all the benefits with minimal code while maintaining thread-safety options if needed later.

---

### Eviction Strategy Pattern

We use the **Strategy Pattern** to allow pluggable eviction policies:

```
+------------------+
| <<interface>>    |
| EvictionStrategy |
+------------------+
| + onAccess()     |
| + onPut()        |
+--------+---------+
         |
         ^
         |
+------------------+     +-----------------------+
| LruEviction      |     | (Future: TtlEviction) |
| Strategy         |     +-----------------------+
+------------------+
| - capacity       |
| + onAccess()     |
| + onPut()        |
+------------------+
```

#### Interface Definition

```java
public interface EvictionStrategy {
    void onAccess(Map<Integer, String> cache, Integer key);
    void onPut(Map<Integer, String> cache, Integer key, String value);
}
```

#### LruEvictionStrategy Implementation

The `LruEvictionStrategy` works in conjunction with `LinkedHashMap`:

- **onAccess()**: When an entry is accessed, it's moved to the end (most recently used)
- **onPut()**: Handles both new insertions and updates to existing keys

The strategy ensures:
1. Updates move the key to the end (most recently used)
2. New insertions that exceed capacity remove the least recently used (first) entry

---

### Prefetch Service

The prefetch service anticipates future data needs by loading adjacent keys into the cache proactively.

```
+------------------+
| <<interface>>    |
|    Prefetch      |
+------------------+
| + prefetch()     |
+--------+---------+
         |
         ^
         |
+------------------+
|  FixedFetch      |
|  (Singleton)     |
+------------------+
| - threshold      |
| - repository     |
+------------------+
```

#### How It Works

When a key is requested and not found in the cache:

1. The value is fetched from the repository (database)
2. The value is cached
3. **Prefetch is triggered**: `FixedFetch` loads keys in the range `[key - threshold, key + threshold]`

**Example**: If threshold = 2 and key = 5 is requested:
- Prefetch fetches keys: 3, 4, 6, 7
- These keys are pre-loaded into the cache
- Future requests for these keys will be O(1) cache hits

#### Why Singleton?

The user requested both Repository and Prefetch to be singletons because:
- They provide **stateless utility functions** (no per-instance state needed)
- Ensures consistent behavior across all Nodes
- Avoids unnecessary object creation overhead
- Only one instance is needed to coordinate caching behavior

---

### Repository Pattern

The Repository acts as an abstraction layer between the cache nodes and the data source:

```
+------------------+
|    Repository    |
|   (Singleton)    |
+------------------+
| - database: Map  |
+------------------+
| + get(key)       |
| + containsKey()  |
+------------------+
```

#### Features

- **Singleton Pattern**: Single instance loads the fake database once
- **Lazy Initialization**: Database is loaded on first access
- **JSON-based Fake Database**: Reads from `fake-db.json`
- **Abstraction**: Provides a clean interface hiding the data source implementation

#### Fake Database (fake-db.json)

```json
{
  "1": "value1",
  "2": "value2",
  "3": "value3",
  ...
  "20": "value20"
}
```

---

### Data Distribution

The system supports dynamic scaling with data redistribution:

```
+------------------+
| <<abstract>>     |
| DataDistribution |
+------------------+
| + additionRearrange()
| + removalRearrange()
+--------+---------+
         |
         ^
         |
+------------------+
| CopyDataDist     |
+------------------+
```

#### Current Implementation: CopyDataDistribution

When a node is **added**:
- Iterates through all existing nodes' caches
- Redistributes keys that now belong to the new node based on the selection strategy

When a node is **removed**:
- Keys from the removed node are redistributed to remaining nodes

---

## Project Setup Guide

### Prerequisites

- **Java**: JDK 11 or higher
- **Maven**: 3.6 or higher
- **Git**: For cloning the repository

### Clone the Repository

```bash
git clone <repository-url>
cd cache
```

### Build the Project

```bash
# Clean and compile
mvn clean compile

# Package as JAR
mvn package
```

### Project Structure

```
cache/
|-- pom.xml                 # Maven configuration
|-- src/main/java/com/cache/
|   |-- ApiServerGateway.java
|   |-- Cluster.java
|   |-- client.java
|   |-- CopyDataDistribution.java
|   |-- DataDistribution.java
|   |-- EvictionStrategy.java
|   |-- FixedFetch.java
|   |-- INodeSelectionStrategy.java
|   |-- LruEvictionStrategy.java
|   |-- ModuloNodeSelectionStrategy.java
|   |-- Node.java
|   |-- Prefetch.java
|   |-- Repository.java
|   |-- fake-db.json
|-- README.md
|-- out/                    # Compiled .class files
```

---

## Running the Application

### Using Maven (Recommended)

```bash
# Run the client
mvn exec:java
```

### Expected Output

```
Value for key 4: value4
Value for key 3: value3
```

---

