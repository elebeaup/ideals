package org.rri.ideals.server.dependency;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.ide.highlighter.JavaFileType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rri.ideals.server.LspPath;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Analyzes dependencies between files in a project.
 * This class provides methods to identify direct and indirect dependencies
 * between files and generate dependency trees.
 */
public class DependencyAnalyzer {
    private static final Logger LOG = Logger.getInstance(DependencyAnalyzer.class);
    private final Project project;
    private final PsiManager psiManager;
    private final JavaPsiFacade javaPsiFacade;

    public DependencyAnalyzer(Project project) {
        this.project = project;
        this.psiManager = PsiManager.getInstance(project);
        this.javaPsiFacade = JavaPsiFacade.getInstance(project);
    }

    /**
     * Gets a dependency tree for a file, showing all files that depend on it
     * (reverse dependencies/incoming references).
     *
     * @param fileUri URI of the file to analyze
     * @return A dependency tree object with both direct and indirect dependencies
     */
    public CompletableFuture<DependencyTree> getReverseDependencyTree(String fileUri) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                LOG.info("Analyzing reverse dependencies for: " + fileUri);
                LspPath path = LspPath.fromLspUri(fileUri);
                VirtualFile virtualFile = path.findVirtualFile();
                if (virtualFile == null) {
                    LOG.warn("Could not find virtual file for URI: " + fileUri);
                    return new DependencyTree(fileUri, Collections.emptyList());
                }

                PsiFile psiFile = psiManager.findFile(virtualFile);
                if (psiFile == null) {
                    LOG.warn("Could not find PSI file for virtual file: " + virtualFile.getPath());
                    return new DependencyTree(fileUri, Collections.emptyList());
                }

                // Find all files that directly depend on this file
                Set<PsiFile> directDependencies = findDependentFiles(psiFile);
                LOG.info("Found " + directDependencies.size() + " direct dependents for: " + fileUri);
                
                // Create dependency nodes
                List<DependencyNode> nodes = directDependencies.stream()
                        .map(file -> {
                            String uri = LspPath.fromVirtualFile(file.getVirtualFile()).toLspUri();
                            // Find the files that depend on this dependency
                            Set<PsiFile> indirectDeps = findDependentFiles(file);
                            indirectDeps.remove(psiFile); // Remove the original file if it appears
                            List<String> indirectUris = indirectDeps.stream()
                                    .map(f -> LspPath.fromVirtualFile(f.getVirtualFile()).toLspUri())
                                    .collect(Collectors.toList());
                            return new DependencyNode(uri, indirectUris);
                        })
                        .collect(Collectors.toList());

                return new DependencyTree(fileUri, nodes);
            } catch (Exception e) {
                LOG.error("Error analyzing reverse dependencies for " + fileUri, e);
                return new DependencyTree(fileUri, Collections.emptyList());
            }
        });
    }

    /**
     * Gets a dependency tree for a file, showing all files it depends on
     * (forward dependencies/outgoing references).
     *
     * @param fileUri URI of the file to analyze
     * @return A dependency tree object with both direct and indirect dependencies
     */
    public CompletableFuture<DependencyTree> getForwardDependencyTree(String fileUri) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                LOG.info("Analyzing forward dependencies for: " + fileUri);
                LspPath path = LspPath.fromLspUri(fileUri);
                VirtualFile virtualFile = path.findVirtualFile();
                if (virtualFile == null) {
                    LOG.warn("Could not find virtual file for URI: " + fileUri);
                    return new DependencyTree(fileUri, Collections.emptyList());
                }

                PsiFile psiFile = psiManager.findFile(virtualFile);
                if (psiFile == null) {
                    LOG.warn("Could not find PSI file for virtual file: " + virtualFile.getPath());
                    return new DependencyTree(fileUri, Collections.emptyList());
                }

                // Find all files this file directly depends on
                Set<PsiFile> directDependencies = findDependedFiles(psiFile);
                LOG.info("Found " + directDependencies.size() + " direct dependencies for: " + fileUri);
                
                // Create dependency nodes
                List<DependencyNode> nodes = directDependencies.stream()
                        .map(file -> {
                            String uri = LspPath.fromVirtualFile(file.getVirtualFile()).toLspUri();
                            // Find the files that this dependency depends on
                            Set<PsiFile> indirectDeps = findDependedFiles(file);
                            indirectDeps.remove(psiFile); // Remove the original file if it appears
                            List<String> indirectUris = indirectDeps.stream()
                                    .map(f -> LspPath.fromVirtualFile(f.getVirtualFile()).toLspUri())
                                    .collect(Collectors.toList());
                            return new DependencyNode(uri, indirectUris);
                        })
                        .collect(Collectors.toList());

                return new DependencyTree(fileUri, nodes);
            } catch (Exception e) {
                LOG.error("Error analyzing forward dependencies for " + fileUri, e);
                return new DependencyTree(fileUri, Collections.emptyList());
            }
        });
    }

    /**
     * Gets a complete dependency graph for the project, showing all file dependencies.
     *
     * @return A map of file URIs to their direct dependencies
     */
    public CompletableFuture<Map<String, List<String>>> getProjectDependencyGraph() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, List<String>> graph = new HashMap<>();
                
                // Get all Java files in the project
                GlobalSearchScope scope = GlobalSearchScope.projectScope(project);
                
                // Use FileTypeIndex to find Java files
                Collection<VirtualFile> javaFiles = new ArrayList<>();
                FileTypeIndex.processFiles(JavaFileType.INSTANCE, file -> {
                    javaFiles.add(file);
                    return true;
                }, scope);
                
                LOG.info("Found " + javaFiles.size() + " Java files in the project");
                
                // For each file, find its dependencies
                for (VirtualFile file : javaFiles) {
                    PsiFile psiFile = psiManager.findFile(file);
                    if (psiFile instanceof PsiJavaFile) {
                        String fileUri = LspPath.fromVirtualFile(file).toLspUri();
                        Set<PsiFile> dependencies = findDependedFiles(psiFile);
                        List<String> dependencyUris = dependencies.stream()
                                .map(depFile -> LspPath.fromVirtualFile(depFile.getVirtualFile()).toLspUri())
                                .collect(Collectors.toList());
                        graph.put(fileUri, dependencyUris);
                        LOG.info("File " + fileUri + " has " + dependencyUris.size() + " dependencies");
                    }
                }
                
                return graph;
            } catch (Exception e) {
                LOG.error("Error generating project dependency graph", e);
                return Collections.emptyMap();
            }
        });
    }

    /**
     * Analyzes the impact of changing a file by finding all files that directly
     * or indirectly depend on it.
     *
     * @param fileUri URI of the file to analyze
     * @return A list of file URIs that would be affected by changes to the specified file
     */
    public CompletableFuture<DependencyImpact> analyzeImpact(String fileUri) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                LOG.info("Analyzing impact for: " + fileUri);
                LspPath path = LspPath.fromLspUri(fileUri);
                VirtualFile virtualFile = path.findVirtualFile();
                if (virtualFile == null) {
                    LOG.warn("Could not find virtual file for URI: " + fileUri);
                    return new DependencyImpact(fileUri, Collections.emptyList(), Collections.emptyList());
                }

                PsiFile psiFile = psiManager.findFile(virtualFile);
                if (psiFile == null) {
                    LOG.warn("Could not find PSI file for virtual file: " + virtualFile.getPath());
                    return new DependencyImpact(fileUri, Collections.emptyList(), Collections.emptyList());
                }

                // Find direct dependencies (files that directly depend on this file)
                Set<PsiFile> directDependencies = findDependentFiles(psiFile);
                List<String> directUris = directDependencies.stream()
                        .map(file -> LspPath.fromVirtualFile(file.getVirtualFile()).toLspUri())
                        .collect(Collectors.toList());
                LOG.info("Found " + directUris.size() + " files directly impacted by: " + fileUri);

                // Find indirect dependencies (files that depend on the direct dependencies)
                Set<PsiFile> indirectDependencies = new HashSet<>();
                for (PsiFile directDep : directDependencies) {
                    Set<PsiFile> transitiveDeps = findDependentFiles(directDep);
                    transitiveDeps.remove(psiFile); // Remove the original file if it appears
                    transitiveDeps.removeAll(directDependencies); // Remove direct dependencies
                    indirectDependencies.addAll(transitiveDeps);
                }
                
                List<String> indirectUris = indirectDependencies.stream()
                        .map(file -> LspPath.fromVirtualFile(file.getVirtualFile()).toLspUri())
                        .collect(Collectors.toList());
                LOG.info("Found " + indirectUris.size() + " files indirectly impacted by: " + fileUri);

                return new DependencyImpact(fileUri, directUris, indirectUris);
            } catch (Exception e) {
                LOG.error("Error analyzing impact for " + fileUri, e);
                return new DependencyImpact(fileUri, Collections.emptyList(), Collections.emptyList());
            }
        });
    }

    /**
     * Finds all files that depend on the specified file.
     *
     * @param file The file to find dependents for
     * @return A set of PsiFiles that depend on the specified file
     */
    @NotNull
    private Set<PsiFile> findDependentFiles(@NotNull PsiFile file) {
        Set<PsiFile> dependentFiles = new HashSet<>();
        
        // For Java files, find all files that import classes from this file
        if (file instanceof PsiJavaFile) {
            PsiJavaFile javaFile = (PsiJavaFile) file;
            
            // Handle package and class identification
            String packageName = javaFile.getPackageName();
            LOG.info("Finding dependents for file in package: " + packageName);
            
            for (PsiClass psiClass : javaFile.getClasses()) {
                String qualifiedName = psiClass.getQualifiedName();
                LOG.info("Finding references to class: " + qualifiedName);
                
                // Manual search across all potential references
                Collection<VirtualFile> allJavaFiles = new ArrayList<>();
                FileTypeIndex.processFiles(JavaFileType.INSTANCE, allJavaFiles::add, GlobalSearchScope.projectScope(project));
                
                for (VirtualFile potentialFile : allJavaFiles) {
                    if (potentialFile.equals(file.getVirtualFile())) {
                        continue; // Skip the file we're analyzing
                    }
                    
                    PsiFile potentialPsiFile = psiManager.findFile(potentialFile);
                    if (potentialPsiFile instanceof PsiJavaFile) {
                        PsiJavaFile potentialJavaFile = (PsiJavaFile) potentialPsiFile;
                        
                        // Check imports first
                        boolean hasImport = false;
                        PsiImportList importList = potentialJavaFile.getImportList();
                        if (importList != null) {
                            for (PsiImportStatement importStmt : importList.getImportStatements()) {
                                String importText = importStmt.getQualifiedName();
                                if (importText != null) {
                                    if (importText.equals(qualifiedName) || // Direct import
                                        (importText.endsWith(".*") && qualifiedName.startsWith(importText.substring(0, importText.length() - 2)))) { // Wildcard import
                                        hasImport = true;
                                        break;
                                    }
                                }
                            }
                        }
                        
                        // If imported, or if in same package (implicit import)
                        if (hasImport || potentialJavaFile.getPackageName().equals(packageName)) {
                            // Look for references to the class name
                            final String className = psiClass.getName();
                            if (className != null) {
                                boolean hasReference = false;
                                String potentialFileText = potentialJavaFile.getText();
                                
                                // Simple text-based check for class name references
                                // This is a fallback approach and not as accurate as PSI-based searching
                                if (potentialFileText.contains(className)) {
                                    hasReference = true;
                                }
                                
                                if (hasReference) {
                                    dependentFiles.add(potentialJavaFile);
                                    LOG.info("Found dependent file: " + potentialJavaFile.getName());
                                }
                            }
                        }
                    }
                }
                
                // Try IntelliJ's reference search as well (may not work in all contexts)
                try {
                    ReferencesSearch.search(psiClass, GlobalSearchScope.projectScope(project)).forEach(reference -> {
                        PsiElement element = reference.getElement();
                        PsiFile containingFile = element.getContainingFile();
                        if (containingFile != null && !containingFile.equals(file)) {
                            dependentFiles.add(containingFile);
                            LOG.info("Found reference to " + psiClass.getName() + " in " + containingFile.getName());
                        }
                    });
                } catch (Exception e) {
                    LOG.warn("Error searching for references to " + psiClass.getName(), e);
                }
            }
        }
        
        return dependentFiles;
    }

    /**
     * Finds all files that the specified file depends on.
     *
     * @param file The file to find dependencies for
     * @return A set of PsiFiles that the specified file depends on
     */
    @NotNull
    private Set<PsiFile> findDependedFiles(@NotNull PsiFile file) {
        Set<PsiFile> dependedFiles = new HashSet<>();
        
        // For Java files, extract import statements and resolve them to files
        if (file instanceof PsiJavaFile) {
            PsiJavaFile javaFile = (PsiJavaFile) file;
            LOG.info("Finding dependencies for: " + javaFile.getName() + " in package " + javaFile.getPackageName());
            
            // Process import statements
            PsiImportList importList = javaFile.getImportList();
            if (importList != null) {
                // Process regular imports
                for (PsiImportStatement importStatement : importList.getImportStatements()) {
                    String importText = importStatement.getQualifiedName();
                    LOG.info("Processing import: " + importText);
                    
                    if (importText != null) {
                        if (importText.endsWith(".*")) {
                            // This is a wildcard import, we need to find all classes in the package
                            String packageName = importText.substring(0, importText.length() - 2);
                            LOG.info("Processing wildcard import for package: " + packageName);
                            
                            // Find all classes in this package
                            PsiPackage psiPackage = javaPsiFacade.findPackage(packageName);
                            if (psiPackage != null) {
                                for (PsiClass psiClass : psiPackage.getClasses()) {
                                    PsiFile containingFile = psiClass.getContainingFile();
                                    if (containingFile != null && !containingFile.equals(file)) {
                                        dependedFiles.add(containingFile);
                                        LOG.info("Found dependency on class: " + psiClass.getName() + " in file: " + containingFile.getName());
                                    }
                                }
                            }
                        } else {
                            // Regular import of a specific class
                            PsiClass importedClass = javaPsiFacade.findClass(importText, GlobalSearchScope.projectScope(project));
                            if (importedClass != null) {
                                PsiFile containingFile = importedClass.getContainingFile();
                                if (containingFile != null && !containingFile.equals(file)) {
                                    dependedFiles.add(containingFile);
                                    LOG.info("Found dependency on class: " + importText + " in file: " + containingFile.getName());
                                }
                            }
                        }
                    }
                }
                
                // Process static imports
                for (PsiImportStaticStatement staticImport : importList.getImportStaticStatements()) {
                    String importText = staticImport.getImportReference() != null ? staticImport.getImportReference().getQualifiedName() : null;
                    LOG.info("Processing static import: " + importText);
                    
                    if (importText != null) {
                        int lastDot = importText.lastIndexOf('.');
                        if (lastDot > 0) {
                            String className = importText.substring(0, lastDot);
                            PsiClass importedClass = javaPsiFacade.findClass(className, GlobalSearchScope.projectScope(project));
                            if (importedClass != null) {
                                PsiFile containingFile = importedClass.getContainingFile();
                                if (containingFile != null && !containingFile.equals(file)) {
                                    dependedFiles.add(containingFile);
                                    LOG.info("Found dependency via static import on class: " + className + " in file: " + containingFile.getName());
                                }
                            }
                        }
                    }
                }
            }
            
            // Also check for direct references to classes not explicitly imported
            // This catches classes in the same package or from java.lang
            file.accept(new JavaRecursiveElementVisitor() {
                @Override
                public void visitReferenceElement(PsiJavaCodeReferenceElement reference) {
                    super.visitReferenceElement(reference);
                    PsiElement resolved = reference.resolve();
                    if (resolved instanceof PsiClass) {
                        PsiFile containingFile = resolved.getContainingFile();
                        if (containingFile != null && !containingFile.equals(file) 
                                && !dependedFiles.contains(containingFile)) {
                            dependedFiles.add(containingFile);
                            LOG.info("Found dependency via direct reference: " + ((PsiClass) resolved).getQualifiedName() 
                                    + " in file: " + containingFile.getName());
                        }
                    }
                }
                
                @Override
                public void visitMethodCallExpression(PsiMethodCallExpression expression) {
                    super.visitMethodCallExpression(expression);
                    PsiMethod method = expression.resolveMethod();
                    if (method != null) {
                        PsiFile containingFile = method.getContainingFile();
                        if (containingFile != null && !containingFile.equals(file)
                                && !dependedFiles.contains(containingFile)) {
                            dependedFiles.add(containingFile);
                            LOG.info("Found dependency via method call: " + method.getName() 
                                    + " in file: " + containingFile.getName());
                        }
                    }
                }
                
                @Override
                public void visitReferenceExpression(PsiReferenceExpression expression) {
                    super.visitReferenceExpression(expression);
                    PsiElement resolved = expression.resolve();
                    if (resolved instanceof PsiField || resolved instanceof PsiMethod) {
                        PsiFile containingFile = resolved.getContainingFile();
                        if (containingFile != null && !containingFile.equals(file)
                                && !dependedFiles.contains(containingFile)) {
                            dependedFiles.add(containingFile);
                            LOG.info("Found dependency via field/method reference: " + expression.getText() 
                                    + " in file: " + containingFile.getName());
                        }
                    }
                }
            });
        }
        
        return dependedFiles;
    }
}