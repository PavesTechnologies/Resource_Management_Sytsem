# RMS Skill Search Endpoint - Design Documentation

## 🎯 Overview
Single search endpoint that accepts user input (skill or subskill name) and returns matching categories, skills, and subskills with full hierarchical context.

**Endpoint**: `GET /api/skill-categories/search?query=java`

---

## 🧠 Functional Requirements

### Search Behavior
- ✅ **Case-insensitive**: `"Java"` matches `"java"`
- ✅ **Partial match**: `"java"` matches `"Java Programming"`
- ✅ **Multi-entity search**: Categories, Skills, and SubSkills
- ✅ **All matches returned**: Not limited to single result type

### Status Rules
- ✅ **Active only**: Only entities with `status = 'ACTIVE'`
- ✅ **Hierarchical filtering**: Inactive/deprecated categories excluded
- ✅ **Data consistency**: Triple status filtering for subskills

### Response Requirements
- ✅ **Unified DTO**: `SkillSearchResultDto` for all result types
- ✅ **Type discrimination**: `CATEGORY`, `SKILL`, `SUBSKILL`
- ✅ **Hierarchical context**: Category → Skill → SubSkill relationships
- ✅ **Error handling**: 404 with `"skill not exist"` message

---

## 🧩 Technical Implementation

### N+1 Query Problem Explained

#### What is N+1 Problem?
The N+1 query problem occurs when:
1. **1 query** fetches the main entities (e.g., categories)
2. **N queries** fetch related entities for each main entity (e.g., skills for each category)
3. **N×M queries** fetch nested entities (e.g., subskills for each skill)

**Example without JOIN FETCH:**
```sql
-- Query 1: Get categories matching "java"
SELECT * FROM skill_category WHERE name LIKE '%java%';

-- Query 2-N: Get skills for each category (N = number of matching categories)
SELECT * FROM skill WHERE category_id = ?;

-- Query N×M: Get subskills for each skill (M = average skills per category)
SELECT * FROM sub_skill WHERE skill_id = ?;
```

**Performance Impact:**
- 10 matching categories → 1 + 10 + (10×5) = **61 queries**
- Database round trips: 61 vs 1
- Response time: 500ms vs 50ms

#### How JOIN FETCH Prevents N+1

**With JOIN FETCH:**
```sql
-- Single query loads complete object graph
SELECT DISTINCT sc FROM SkillCategory sc 
LEFT JOIN FETCH sc.skills s 
LEFT JOIN FETCH s.subSkills ss 
WHERE sc.name LIKE '%java%';
```

**Benefits:**
- **1 query** instead of N+1 queries
- **Single database round trip**
- **Consistent data snapshot**
- **No lazy loading surprises**

---

### Repository Layer Performance Analysis

#### Query 1: Category Search
```sql
SELECT DISTINCT sc FROM SkillCategory sc 
LEFT JOIN FETCH sc.skills s 
LEFT JOIN FETCH s.subSkills ss 
WHERE sc.status = 'ACTIVE' 
AND LOWER(sc.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
ORDER BY sc.name
```

**Time Complexity**: `O(N + M + P)`
- **N** = Number of categories
- **M** = Average skills per category
- **P** = Average subskills per skill

**Performance Characteristics:**
- ✅ **Prevents N+1**: Single query loads hierarchy
- ✅ **DISTINCT**: Prevents cartesian product duplicates
- ⚠️ **Memory usage**: Higher due to eager loading
- ⚠️ **LIKE with wildcards**: Prevents index usage

#### Query 2: Skill Search
```sql
SELECT DISTINCT s FROM Skill s 
LEFT JOIN FETCH s.category sc 
LEFT JOIN FETCH s.subSkills ss 
WHERE s.status = 'ACTIVE' AND sc.status = 'ACTIVE' 
AND LOWER(s.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
ORDER BY sc.name, s.name
```

**Time Complexity**: `O(S + P)`
- **S** = Number of skills
- **P** = Average subskills per skill

**Performance Characteristics:**
- ✅ **Dual status filtering**: Ensures data consistency
- ✅ **Optimized ordering**: Hierarchical sort
- ✅ **Moderate memory**: Skills typically less than categories

#### Query 3: SubSkill Search
```sql
SELECT DISTINCT ss FROM SubSkill ss 
LEFT JOIN FETCH ss.skill s 
LEFT JOIN FETCH s.category sc 
WHERE ss.status = 'ACTIVE' AND s.status = 'ACTIVE' AND sc.status = 'ACTIVE' 
AND LOWER(ss.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
ORDER BY sc.name, s.name, ss.name
```

**Time Complexity**: `O(SS)`
- **SS** = Number of subskills

**Performance Characteristics:**
- ✅ **Most efficient**: Leaf nodes, minimal joins
- ✅ **Triple filtering**: Complete hierarchy validation
- ✅ **Best performance**: Typically most specific searches

---

### Service Layer Architecture

#### Search Flow
```java
searchSkills(query) {
    1. Input validation (null/empty check)
    2. Execute 3 repository queries in parallel
    3. Map entities to DTOs with builder pattern
    4. Handle exceptions gracefully
    5. Return unified result list
}
```

#### DTO Mapping Strategy
```java
// Category result
SkillSearchResultDto.builder()
    .type("CATEGORY")
    .id(category.getId())
    .name(category.getName())
    .categoryName(null)      // Not applicable
    .parentSkillName(null)   // Not applicable
    .subSkills(null)         // Not applicable
    .build()

// Skill result
SkillSearchResultDto.builder()
    .type("SKILL")
    .id(skill.getId())
    .name(skill.getName())
    .categoryName(skill.getCategory().getName())
    .parentSkillName(null)   // Not applicable
    .subSkills(extractSubSkillNames(skill))
    .build()

// SubSkill result
SkillSearchResultDto.builder()
    .type("SUBSKILL")
    .id(subSkill.getId())
    .name(subSkill.getName())
    .categoryName(subSkill.getSkill().getCategory().getName())
    .parentSkillName(subSkill.getSkill().getName())
    .subSkills(null)         // Not applicable
    .build()
```

---

### Controller Layer Design

#### Endpoint Specification
```java
@GetMapping("/search")
public ResponseEntity<ApiResponse<List<SkillSearchResultDto>>> searchSkills(
        @RequestParam String query)
```

#### Response Matrix
| Scenario | HTTP Status | Response Body |
|----------|-------------|---------------|
| Empty query | 400 | `{"success": false, "message": "Search query cannot be empty"}` |
| No matches | 404 | `{"success": false, "message": "skill not exist"}` |
| Success | 200 | `{"success": true, "message": "Skills found successfully", "data": [...]}` |

---

## 📊 Performance Benchmarks

### Dataset Sizes
| Metric | Small | Medium | Large |
|--------|-------|--------|-------|
| Categories | 100 | 1,000 | 10,000 |
| Skills | 1,000 | 10,000 | 100,000 |
| SubSkills | 5,000 | 50,000 | 500,000 |

### Expected Performance
| Dataset | Current Implementation | Optimized Version |
|---------|----------------------|------------------|
| Small | ~10-50ms | ~5-20ms |
| Medium | ~100-500ms | ~50-200ms |
| Large | ~1-5s | ~500ms-2s |

### Optimization Recommendations

#### 1. Database Indexes
```sql
-- Essential for performance
CREATE INDEX idx_skill_category_name_active ON skill_category(name, status);
CREATE INDEX idx_skill_name_active ON skill(name, status);
CREATE INDEX idx_subskill_name_active ON sub_skill(name, status);

-- Composite indexes for common queries
CREATE INDEX idx_skill_category_status ON skill(category_id, status);
CREATE INDEX idx_subskill_skill_status ON sub_skill(skill_id, status);
```

#### 2. Full-Text Search (Large Datasets)
```sql
-- PostgreSQL example
ALTER TABLE skill_category ADD COLUMN search_vector tsvector;
UPDATE skill_category SET search_vector = to_tsvector(name);
CREATE INDEX idx_search_vector ON skill_category USING gin(search_vector);

-- Query becomes:
SELECT * FROM skill_category WHERE search_vector @@ plainto_tsquery('java')
```

#### 3. Pagination (Very Large Datasets)
```java
Page<SkillSearchResultDto> searchSkills(String query, Pageable pageable);
```

---

## 🚨 Production Considerations

### Error Handling
- ✅ **Input validation**: Empty/null queries
- ✅ **Exception handling**: Graceful degradation
- ✅ **Logging**: Error tracking for debugging

### Security
- ✅ **SQL Injection**: Parameterized queries
- ✅ **Data exposure**: Only active entities returned
- ⚠️ **Rate limiting**: Consider for public APIs

### Monitoring
- **Query performance**: Track slow queries
- **Search patterns**: Popular search terms
- **Error rates**: Failed search attempts

---

## ✅ Validation Checklist

### Functional Requirements
- [x] Case-insensitive search
- [x] Partial matching
- [x] Multi-entity search
- [x] Active-only filtering
- [x] Hierarchical context
- [x] 404 error handling

### Technical Requirements
- [x] N+1 query prevention
- [x] JOIN FETCH optimization
- [x] DTO pattern usage
- [x] Repository layer separation
- [x] Service layer orchestration
- [x] Controller layer validation

### Performance Requirements
- [x] Single database round trip per entity type
- [x] DISTINCT for duplicate prevention
- [x] Proper indexing recommendations
- [x] Scalability considerations

---

## 🔍 Usage Examples

### Basic Search
```bash
GET /api/skill-categories/search?query=java
```

### Response Example
```json
{
  "success": true,
  "message": "Skills found successfully",
  "data": [
    {
      "type": "CATEGORY",
      "id": "123e4567-e89b-12d3-a456-426614174000",
      "name": "Java Programming",
      "description": "Java related skills and technologies",
      "categoryName": null,
      "parentSkillName": null,
      "subSkills": null,
      "status": "ACTIVE"
    },
    {
      "type": "SKILL",
      "id": "123e4567-e89b-12d3-a456-426614174001",
      "name": "Core Java",
      "description": "Java fundamentals and syntax",
      "categoryName": "Java Programming",
      "parentSkillName": null,
      "subSkills": ["Syntax", "OOP Concepts", "Collections"],
      "status": "ACTIVE"
    },
    {
      "type": "SUBSKILL",
      "id": "123e4567-e89b-12d3-a456-426614174002",
      "name": "Java Streams",
      "description": "Stream API and functional programming",
      "categoryName": "Java Programming",
      "parentSkillName": "Core Java",
      "subSkills": null,
      "status": "ACTIVE"
    }
  ]
}
```

### No Results
```bash
GET /api/skill-categories/search?query=nonexistent
```

```json
{
  "success": false,
  "message": "skill not exist",
  "data": null
}
```

---

## 📈 Future Enhancements

### Phase 2 Optimizations
1. **Caching Layer**: Redis for frequent searches
2. **Search Analytics**: Track popular search terms
3. **Autocomplete**: Real-time suggestions
4. **Faceted Search**: Filter by category, skill type

### Phase 3 Scalability
1. **Elasticsearch**: Full-text search capabilities
2. **Microservices**: Separate search service
3. **GraphQL**: Flexible query interface
4. **Machine Learning**: Search relevance ranking

---

*This documentation provides comprehensive coverage of the search endpoint design, implementation details, and production considerations for the RMS skill framework.*
