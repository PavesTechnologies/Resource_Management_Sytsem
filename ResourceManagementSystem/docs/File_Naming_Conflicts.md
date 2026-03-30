# 📁 FILE NAMING CONFLICTS IDENTIFIED

## 🚨 **SPRING CONTEXT CONFLICTS**

### **Duplicate Service Classes Found**

**1. AvailabilityLedgerAsyncService (CONFLICT)**
```
├── Original: src/main/java/com/service_imple/allocation_service_imple/AvailabilityLedgerAsyncService.java
└── Refactored: src/main/java/com/service_imple/allocation_service_imple/AvailabilityLedgerAsyncServiceRefactored.java
```

**2. AvailabilityCalculationService (DIFFERENT PURPOSE)**
```
├── Main Interface: src/main/java/com/service_interface/availability_service_interface/AvailabilityCalculationService.java
├── Main Implementation: src/main/java/com/service_imple/availability_service_impl/AvailabilityCalculationServiceImpl.java
└── Separate Service: src/main/java/com/service_imple/ledger_service_impl/AvailabilityCalculationService.java
    └── Purpose: ResourceAvailabilityLedgerDaily entities (different from main ledger)
```

---

## ⚠️ **SPRING BEAN CONFLICTS**

### **Problem:**
Both `AvailabilityLedgerAsyncService.java` and `AvailabilityLedgerAsyncServiceRefactored.java` have `@Service` annotation

**Spring Context Issue:**
- Multiple beans with similar names
- Potential injection ambiguity
- Runtime startup conflicts

### **Current Usage Pattern:**
```java
// In AllocationServiceImple.java:
@Autowired
private AvailabilityLedgerAsyncService availabilityLedgerAsyncService; // Which one gets injected?

// In RoleOffServiceImpl.java:
@Autowired  
private AvailabilityLedgerAsyncServiceRefactored availabilityLedgerAsyncService; // Explicitly using refactored version
```

---

## ✅ **RESOLUTION OPTIONS**

### **Option 1: Rename Original (RECOMMENDED)**
```bash
# Rename original to avoid conflicts
mv AvailabilityLedgerAsyncService.java → AvailabilityLedgerAsyncServiceLegacy.java
```

### **Option 2: Use @Primary Annotation**
```java
@Service("availabilityLedgerAsyncServiceRefactored")
@Primary
public class AvailabilityLedgerAsyncServiceRefactored {
    // Spring will prefer this bean
}
```

### **Option 3: Use @Qualifier**
```java
@Autowired
@Qualifier("availabilityLedgerAsyncServiceRefactored")
private AvailabilityLedgerAsyncService availabilityLedgerAsyncService;
```

---

## 🎯 **RECOMMENDED ACTION**

### **Immediate Fix:**
1. **Rename original** to `AvailabilityLedgerAsyncServiceLegacy.java`
2. **Update imports** in files using the old service
3. **Keep refactored version** as primary service
4. **Test Spring context** startup

### **Files to Update:**
- `AllocationServiceImple.java` - Update import/autowiring
- Any other files using the original service
- Consider deprecating original service

---

## 📊 **IMPACT ANALYSIS**

| Issue | Risk | Resolution |
|--------|--------|------------|
| Bean naming conflict | High | Rename original service |
| Injection ambiguity | Medium | Use @Qualifier or @Primary |
| Runtime startup failure | Critical | Fix bean conflicts |
| Maintenance confusion | Medium | Clear naming convention |

---

## 🚀 **NEXT STEPS**

1. **Backup current state**
2. **Rename original service** to legacy name
3. **Update all references** to use refactored version
4. **Test Spring context** loads correctly
5. **Verify functionality** works as expected

**This naming conflict should be resolved to prevent Spring context issues and ensure clean deployment.**
