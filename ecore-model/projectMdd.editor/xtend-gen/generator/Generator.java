package generator;

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import generator.Helpers;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtend2.lib.StringConcatenation;
import org.eclipse.xtext.xbase.lib.Exceptions;
import org.eclipse.xtext.xbase.lib.Functions.Function1;
import org.eclipse.xtext.xbase.lib.IterableExtensions;
import org.eclipse.xtext.xbase.lib.IteratorExtensions;
import org.eclipse.xtext.xbase.lib.StringExtensions;
import projectMdd.Attribute;
import projectMdd.Backend;
import projectMdd.DataType;
import projectMdd.Entity;
import projectMdd.EnumAttribute;
import projectMdd.Relation;
import projectMdd.RelationType;
import projectMdd.TypeAttribute;

/**
 * The generator for ecore files.
 * 
 * @author Marco Richter
 */
@SuppressWarnings("all")
public class Generator {
  /**
   * The path where to generate the Java files.
   */
  public static final String SOURCE_FOLDER_PATH = "src-gen/main";
  
  /**
   * The base package name. Needs the succeeding dot!
   */
  public static final String PACKAGE = "de.thm.dbiGenerator";
  
  public static final String PACKAGE_PATH = ("/" + Generator.PACKAGE.replaceAll("\\.", "/"));
  
  public static final String COMPLETE_PATH = (Generator.SOURCE_FOLDER_PATH + Generator.PACKAGE_PATH);
  
  public static final String EXTENDED_META_DATA = "http:///org/eclipse/emf/ecore/util/ExtendedMetaData";
  
  public static final String MAX_INCLUSIVE = "maxInclusive";
  
  public static final String MIN_INCLUSIVE = "minInclusive";
  
  /**
   * Creates a file (containing the content-CharSequence) within the given IFolder.
   */
  public void createFile(final IFolder folder, final String fileName, final boolean overrideFile, final CharSequence content, final IProgressMonitor progressMonitor) {
    try {
      boolean _isCanceled = progressMonitor.isCanceled();
      if (_isCanceled) {
        throw new RuntimeException("Progress canceled");
      }
      boolean _exists = folder.exists();
      boolean _not = (!_exists);
      if (_not) {
        folder.create(true, true, null);
      }
      IFile iFile = folder.getFile(fileName);
      if ((iFile.exists() && true)) {
        iFile.delete(true, null);
      }
      boolean _exists_1 = iFile.exists();
      boolean _not_1 = (!_exists_1);
      if (_not_1) {
        byte[] _bytes = content.toString().getBytes();
        InputStream source = new ByteArrayInputStream(_bytes);
        iFile.create(source, true, null);
      }
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  /**
   * Starts the generation of the given Resource file in the given IProject.
   */
  public void doGenerate(final Resource resourceEcore, final IProject project, final IProgressMonitor progressMonitor) {
    try {
      progressMonitor.beginTask("Generating Java code.", 2);
      progressMonitor.subTask("Creating folders.");
      String path = "";
      String[] _split = Generator.COMPLETE_PATH.split("/");
      for (final String subPath : _split) {
        {
          String _path = path;
          path = (_path + (subPath + "/"));
          this.getAndCreateFolder(project, path);
        }
      }
      progressMonitor.subTask("Generating Entities");
      this.doGenerate(project, IteratorExtensions.<Backend>head(Iterators.<Backend>filter(resourceEcore.getAllContents(), Backend.class)), progressMonitor);
      this.makeProgressAndCheckCanceled(progressMonitor);
      progressMonitor.done();
    } catch (final Throwable _t) {
      if (_t instanceof Exception) {
        final Exception ex = (Exception)_t;
        ex.printStackTrace();
      } else {
        throw Exceptions.sneakyThrow(_t);
      }
    }
  }
  
  public void makeProgressAndCheckCanceled(final IProgressMonitor monitor) {
    monitor.worked(1);
    boolean _isCanceled = monitor.isCanceled();
    if (_isCanceled) {
      throw new RuntimeException("Progress canceled");
    }
  }
  
  public IFolder getAndCreateFolder(final IProject project, final String path) {
    try {
      IFolder folder = project.getFolder(path);
      boolean _exists = folder.exists();
      boolean _not = (!_exists);
      if (_not) {
        folder.create(true, true, null);
      }
      return folder;
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  public void doGenerate(final IProject project, final EObject rootElement, final IProgressMonitor progressMonitor) {
    Backend backend = ((Backend) rootElement);
    IFolder sourceFolder = this.getAndCreateFolder(project, Generator.SOURCE_FOLDER_PATH.split("/")[0]);
    IFolder resourceFolder = this.getAndCreateFolder(project, (Generator.SOURCE_FOLDER_PATH + "/resources"));
    IFolder packageFolder = this.getAndCreateFolder(project, Generator.COMPLETE_PATH);
    IFolder entityFolder = this.getAndCreateFolder(project, (Generator.COMPLETE_PATH + "/entities"));
    IFolder repoFolder = this.getAndCreateFolder(project, (Generator.COMPLETE_PATH + "/repositories"));
    IFolder pageFolder = this.getAndCreateFolder(project, (Generator.COMPLETE_PATH + "/pages"));
    IFolder editorFolder = this.getAndCreateFolder(project, (Generator.COMPLETE_PATH + "/editors"));
    IFolder detailsFolder = this.getAndCreateFolder(project, (Generator.COMPLETE_PATH + "/details"));
    this.createFile(sourceFolder, "pom.xml", true, this.genPom(backend), progressMonitor);
    this.createFile(resourceFolder, "application.properties", true, this.genApplicationProperties(backend), progressMonitor);
    String _projectName = backend.getProjectName();
    String _plus = (_projectName + "Application.java");
    this.createFile(packageFolder, _plus, true, this.genApplicationClass(backend), progressMonitor);
    this.createFile(packageFolder, "WebSecurityConfig.java", true, this.genWebsecurity(backend), progressMonitor);
    this.createFile(packageFolder, "ServletInitializer.java", true, this.genServletInitializer(), progressMonitor);
    this.createFile(packageFolder, "MainView.java", true, this.genMainView(backend), progressMonitor);
    this.createFile(packageFolder, "ChangeHandler.java", true, this.genChangeHandler(), progressMonitor);
    this.createFile(packageFolder, "AccessDeniedView.java", true, this.genAccessDenied(), progressMonitor);
    this.createFile(packageFolder, "LoginView.java", true, this.genLoginView(), progressMonitor);
    EList<Entity> _entities = backend.getEntities();
    for (final Entity entity : _entities) {
      {
        String _name = entity.getName();
        String _plus_1 = (_name + ".java");
        this.createFile(entityFolder, _plus_1, true, this.genEntityExtensionClass(entity), progressMonitor);
        boolean _isTransient = entity.isTransient();
        boolean _not = (!_isTransient);
        if (_not) {
          String _name_1 = entity.getName();
          String _plus_2 = (_name_1 + "Gen.java");
          this.createFile(entityFolder, _plus_2, true, this.genEntityClass(entity), progressMonitor);
          String _name_2 = entity.getName();
          String _plus_3 = (_name_2 + "Repository.java");
          this.createFile(repoFolder, _plus_3, true, this.genEntityRepo(entity), progressMonitor);
          boolean _isDisplay = entity.isDisplay();
          if (_isDisplay) {
            String _name_3 = entity.getName();
            String _plus_4 = (_name_3 + "GridPage.java");
            this.createFile(pageFolder, _plus_4, true, this.genEntityGridPage(entity), progressMonitor);
            String _name_4 = entity.getName();
            String _plus_5 = (_name_4 + "Editor.java");
            this.createFile(editorFolder, _plus_5, true, this.genEntityEditor(entity), progressMonitor);
          }
        } else {
          String _name_5 = entity.getName();
          String _plus_6 = (_name_5 + "Gen.java");
          this.createFile(entityFolder, _plus_6, true, this.genTransientEntityClass(entity), progressMonitor);
        }
      }
    }
  }
  
  public CharSequence genPom(final Backend backend) {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
    _builder.newLine();
    _builder.append("<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd\">");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("<modelVersion>4.0.0</modelVersion>");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("<parent>");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("<groupId>org.springframework.boot</groupId>");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("<artifactId>spring-boot-starter-parent</artifactId>");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("<version>2.3.1.RELEASE</version>");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("<relativePath/>");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("</parent>");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("<groupId>");
    String _projectName = backend.getProjectName();
    _builder.append(_projectName, "\t");
    _builder.append("</groupId>");
    _builder.newLineIfNotEmpty();
    _builder.append("\t");
    _builder.append("<artifactId>");
    String _projectName_1 = backend.getProjectName();
    _builder.append(_projectName_1, "\t");
    _builder.append("</artifactId>");
    _builder.newLineIfNotEmpty();
    _builder.append("\t");
    _builder.append("<version>0.0.1-SNAPSHOT</version>");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("<packaging>war</packaging>");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("<name>");
    String _projectName_2 = backend.getProjectName();
    _builder.append(_projectName_2, "\t");
    _builder.append("</name>");
    _builder.newLineIfNotEmpty();
    _builder.append("\t");
    _builder.append("<description>");
    String _projectDescription = backend.getProjectDescription();
    _builder.append(_projectDescription, "\t");
    _builder.append("</description>");
    _builder.newLineIfNotEmpty();
    _builder.newLine();
    _builder.append("\t");
    _builder.append("<properties>");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("<java.version>14</java.version>");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("<vaadin.version>16.0.1</vaadin.version>");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("</properties>");
    _builder.newLine();
    _builder.newLine();
    _builder.append("\t");
    _builder.append("<dependencies>");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("<!-- Spring -->");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("<dependency>");
    _builder.newLine();
    _builder.append("\t\t\t");
    _builder.append("<groupId>org.springframework.boot</groupId>");
    _builder.newLine();
    _builder.append("\t\t\t");
    _builder.append("<artifactId>spring-boot-starter-data-jpa</artifactId>");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("</dependency>");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("<dependency>");
    _builder.newLine();
    _builder.append("\t\t\t");
    _builder.append("<groupId>org.springframework.boot</groupId>");
    _builder.newLine();
    _builder.append("\t\t\t");
    _builder.append("<artifactId>spring-boot-starter-security</artifactId>");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("</dependency>");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("<dependency>");
    _builder.newLine();
    _builder.append("\t\t\t");
    _builder.append("<groupId>org.springframework.boot</groupId>");
    _builder.newLine();
    _builder.append("\t\t\t");
    _builder.append("<artifactId>spring-boot-starter-tomcat</artifactId>");
    _builder.newLine();
    _builder.append("\t\t\t");
    _builder.append("<scope>provided</scope>");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("</dependency>");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("<dependency>");
    _builder.newLine();
    _builder.append("\t\t\t");
    _builder.append("<groupId>org.springframework.boot</groupId>");
    _builder.newLine();
    _builder.append("\t\t\t");
    _builder.append("<artifactId>spring-boot-starter-test</artifactId>");
    _builder.newLine();
    _builder.append("\t\t\t");
    _builder.append("<scope>test</scope>");
    _builder.newLine();
    _builder.append("\t\t\t");
    _builder.append("<exclusions>");
    _builder.newLine();
    _builder.append("\t\t\t\t");
    _builder.append("<exclusion>");
    _builder.newLine();
    _builder.append("\t\t\t\t\t");
    _builder.append("<groupId>org.junit.vintage</groupId>");
    _builder.newLine();
    _builder.append("\t\t\t\t\t");
    _builder.append("<artifactId>junit-vintage-engine</artifactId>");
    _builder.newLine();
    _builder.append("\t\t\t\t");
    _builder.append("</exclusion>");
    _builder.newLine();
    _builder.append("\t\t\t");
    _builder.append("</exclusions>");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("</dependency>");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("<dependency>");
    _builder.newLine();
    _builder.append("\t\t\t");
    _builder.append("<groupId>org.springframework.security</groupId>");
    _builder.newLine();
    _builder.append("\t\t\t");
    _builder.append("<artifactId>spring-security-test</artifactId>");
    _builder.newLine();
    _builder.append("\t\t\t");
    _builder.append("<scope>test</scope>");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("</dependency>");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("<!-- vaadin -->");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("<dependency>");
    _builder.newLine();
    _builder.append("\t\t\t");
    _builder.append("<groupId>com.vaadin</groupId>");
    _builder.newLine();
    _builder.append("\t\t\t");
    _builder.append("<artifactId>vaadin-spring-boot-starter</artifactId>");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("</dependency>");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("<!-- database -->");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("<dependency>");
    _builder.newLine();
    _builder.append("\t\t\t");
    _builder.append("<groupId>mysql</groupId>");
    _builder.newLine();
    _builder.append("\t\t\t");
    _builder.append("<artifactId>mysql-connector-java</artifactId>");
    _builder.newLine();
    _builder.append("\t\t\t");
    _builder.append("<scope>runtime</scope>");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("</dependency>");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("<!-- miscellaneous -->");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("<dependency>");
    _builder.newLine();
    _builder.append("\t\t    ");
    _builder.append("<groupId>org.projectlombok</groupId>");
    _builder.newLine();
    _builder.append("\t\t    ");
    _builder.append("<artifactId>lombok</artifactId>");
    _builder.newLine();
    _builder.append("\t\t    ");
    _builder.append("<version>1.18.12</version>");
    _builder.newLine();
    _builder.append("\t\t    ");
    _builder.append("<scope>provided</scope>");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("</dependency>");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("</dependencies>");
    _builder.newLine();
    _builder.newLine();
    _builder.append("\t");
    _builder.append("<dependencyManagement>");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("<dependencies>");
    _builder.newLine();
    _builder.append("\t\t\t");
    _builder.append("<dependency>");
    _builder.newLine();
    _builder.append("\t\t\t\t");
    _builder.append("<groupId>com.vaadin</groupId>");
    _builder.newLine();
    _builder.append("\t\t\t\t");
    _builder.append("<artifactId>vaadin-bom</artifactId>");
    _builder.newLine();
    _builder.append("\t\t\t\t");
    _builder.append("<version>${vaadin.version}</version>");
    _builder.newLine();
    _builder.append("\t\t\t\t");
    _builder.append("<type>pom</type>");
    _builder.newLine();
    _builder.append("\t\t\t\t");
    _builder.append("<scope>import</scope>");
    _builder.newLine();
    _builder.append("\t\t\t");
    _builder.append("</dependency>");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("</dependencies>");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("</dependencyManagement>");
    _builder.newLine();
    _builder.newLine();
    _builder.append("\t");
    _builder.append("<build>");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("<plugins>");
    _builder.newLine();
    _builder.append("\t\t\t");
    _builder.append("<plugin>");
    _builder.newLine();
    _builder.append("\t\t\t\t");
    _builder.append("<groupId>org.springframework.boot</groupId>");
    _builder.newLine();
    _builder.append("\t\t\t\t");
    _builder.append("<artifactId>spring-boot-maven-plugin</artifactId>");
    _builder.newLine();
    _builder.append("\t\t\t");
    _builder.append("</plugin>");
    _builder.newLine();
    _builder.append("\t\t\t");
    _builder.append("<plugin>");
    _builder.newLine();
    _builder.append("\t\t\t\t");
    _builder.append("<groupId>com.vaadin</groupId>");
    _builder.newLine();
    _builder.append("\t\t\t\t");
    _builder.append("<artifactId>vaadin-maven-plugin</artifactId>");
    _builder.newLine();
    _builder.append("\t\t\t\t");
    _builder.append("<version>${vaadin.version}</version>");
    _builder.newLine();
    _builder.append("\t\t\t\t");
    _builder.append("<executions>");
    _builder.newLine();
    _builder.append("\t\t\t\t\t");
    _builder.append("<execution>");
    _builder.newLine();
    _builder.append("\t\t\t\t\t\t");
    _builder.append("<goals>");
    _builder.newLine();
    _builder.append("\t\t\t\t\t\t\t");
    _builder.append("<goal>prepare-frontend</goal>");
    _builder.newLine();
    _builder.append("\t\t\t\t\t\t");
    _builder.append("</goals>");
    _builder.newLine();
    _builder.append("\t\t\t\t\t");
    _builder.append("</execution>");
    _builder.newLine();
    _builder.append("\t\t\t\t");
    _builder.append("</executions>");
    _builder.newLine();
    _builder.append("\t\t\t");
    _builder.append("</plugin>");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("</plugins>");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("</build>");
    _builder.newLine();
    _builder.newLine();
    _builder.append("</project>");
    _builder.newLine();
    return _builder;
  }
  
  public CharSequence genApplicationProperties(final Backend backend) {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("# DATABASE");
    _builder.newLine();
    _builder.append("spring.jpa.hibernate.ddl-auto=create");
    _builder.newLine();
    _builder.append("spring.datasource.url=jdbc:mysql://");
    String _host = backend.getDatabase().getHost();
    _builder.append(_host);
    _builder.append(":");
    String _port = backend.getDatabase().getPort();
    _builder.append(_port);
    _builder.append("/");
    String _schema = backend.getDatabase().getSchema();
    _builder.append(_schema);
    _builder.append("?useSSL=false&allowPublicKeyRetrieval=true");
    _builder.newLineIfNotEmpty();
    _builder.append("spring.datasource.username=");
    String _username = backend.getDatabase().getUsername();
    _builder.append(_username);
    _builder.newLineIfNotEmpty();
    _builder.append("spring.datasource.password=");
    String _password = backend.getDatabase().getPassword();
    _builder.append(_password);
    _builder.newLineIfNotEmpty();
    _builder.append("spring.datasource.driver-class-name=com.mysql.jdbc.Driver");
    _builder.newLine();
    _builder.append("spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect");
    _builder.newLine();
    _builder.newLine();
    _builder.append("# LOGGING");
    _builder.newLine();
    _builder.append("logging.level.root=DEBUG");
    _builder.newLine();
    _builder.append("org.springframework.web.filter.CommonsRequestLoggingFilter=DEBUG");
    _builder.newLine();
    return _builder;
  }
  
  public CharSequence genApplicationClass(final Backend backend) {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("package ");
    _builder.append(Generator.PACKAGE);
    _builder.append(";");
    _builder.newLineIfNotEmpty();
    _builder.newLine();
    CharSequence _entitiesAsImports = Helpers.getEntitiesAsImports(backend, Generator.PACKAGE);
    _builder.append(_entitiesAsImports);
    _builder.newLineIfNotEmpty();
    CharSequence _reposAsImports = Helpers.getReposAsImports(backend, Generator.PACKAGE);
    _builder.append(_reposAsImports);
    _builder.newLineIfNotEmpty();
    _builder.append("import org.slf4j.Logger;");
    _builder.newLine();
    _builder.append("import org.slf4j.LoggerFactory;");
    _builder.newLine();
    _builder.append("import org.springframework.boot.CommandLineRunner;");
    _builder.newLine();
    _builder.append("import org.springframework.boot.SpringApplication;");
    _builder.newLine();
    _builder.append("import org.springframework.boot.autoconfigure.SpringBootApplication;");
    _builder.newLine();
    _builder.append("import org.springframework.context.annotation.Bean;");
    _builder.newLine();
    _builder.newLine();
    _builder.append("import java.io.IOException;");
    _builder.newLine();
    _builder.append("import java.util.HashSet;");
    _builder.newLine();
    _builder.newLine();
    _builder.append("@SpringBootApplication");
    _builder.newLine();
    _builder.append("public class ");
    String _firstUpper = StringExtensions.toFirstUpper(backend.getProjectName());
    _builder.append(_firstUpper);
    _builder.append("Application {");
    _builder.newLineIfNotEmpty();
    _builder.newLine();
    _builder.append("    ");
    _builder.append("private static final Logger log = LoggerFactory.getLogger(");
    String _firstUpper_1 = StringExtensions.toFirstUpper(backend.getProjectName());
    _builder.append(_firstUpper_1, "    ");
    _builder.append("Application.class);");
    _builder.newLineIfNotEmpty();
    _builder.append("    ");
    _builder.newLine();
    _builder.append("    ");
    _builder.append("private static final String CONTAINER_NAME = \"");
    String _projectName = backend.getProjectName();
    _builder.append(_projectName, "    ");
    _builder.append("\";");
    _builder.newLineIfNotEmpty();
    _builder.append("    ");
    _builder.append("private static final String CONTAINER_DATABASE_PASSWORD = \"");
    String _password = backend.getDatabase().getPassword();
    _builder.append(_password, "    ");
    _builder.append("\";");
    _builder.newLineIfNotEmpty();
    _builder.append("    ");
    _builder.append("private static final String CONTAINER_DATABASE_NAME = \"");
    String _schema = backend.getDatabase().getSchema();
    _builder.append(_schema, "    ");
    _builder.append("\";");
    _builder.newLineIfNotEmpty();
    _builder.newLine();
    _builder.append("    ");
    _builder.append("public static void main(String[] args) {");
    _builder.newLine();
    _builder.append("        ");
    _builder.append("createMySQLContainer(CONTAINER_NAME, CONTAINER_DATABASE_PASSWORD, CONTAINER_DATABASE_NAME);");
    _builder.newLine();
    _builder.append("        ");
    _builder.append("startMySQLContainer(CONTAINER_DATABASE_NAME);");
    _builder.newLine();
    _builder.append("     \t");
    _builder.append("SpringApplication.run(");
    String _firstUpper_2 = StringExtensions.toFirstUpper(backend.getProjectName());
    _builder.append(_firstUpper_2, "     \t");
    _builder.append("Application.class, args);");
    _builder.newLineIfNotEmpty();
    _builder.append("    ");
    _builder.append("}");
    _builder.newLine();
    _builder.newLine();
    _builder.append("    ");
    _builder.append("public static void createMySQLContainer(String containerName, String databasePassword, String databaseName) {");
    _builder.newLine();
    _builder.append("            ");
    _builder.append("try {");
    _builder.newLine();
    _builder.append("                ");
    _builder.append("log.info(\"Checking if container {} exists.\", containerName);");
    _builder.newLine();
    _builder.append("                ");
    _builder.append("Process check = Runtime.getRuntime().exec(\"docker inspect -f \'{{.State.Running}}\' \" + containerName);");
    _builder.newLine();
    _builder.append("                ");
    _builder.append("String res = String.valueOf(check.getInputStream());");
    _builder.newLine();
    _builder.append("                ");
    _builder.append("log.info(\"Container exists: {}\", res);");
    _builder.newLine();
    _builder.append("                ");
    _builder.append("check.getOutputStream().close();");
    _builder.newLine();
    _builder.append("                ");
    _builder.append("if (!res.contains(\"true\")) {");
    _builder.newLine();
    _builder.append("                    ");
    _builder.append("log.info(\"Creating container {}.\", containerName);");
    _builder.newLine();
    _builder.append("                    ");
    _builder.append("Process run = Runtime.getRuntime()");
    _builder.newLine();
    _builder.append("                            ");
    _builder.append(".exec(\"docker run -p ");
    String _port = backend.getDatabase().getPort();
    _builder.append(_port, "                            ");
    _builder.append(":3306 --name \" + containerName + \" -e MYSQL_ROOT_PASSWORD=\"");
    _builder.newLineIfNotEmpty();
    _builder.append("                                    ");
    _builder.append("+ databasePassword + \" -e MYSQL_DATABASE=\" + databaseName + \" -d mysql:latest\");");
    _builder.newLine();
    _builder.append("                    ");
    _builder.append("run.getOutputStream().close();");
    _builder.newLine();
    _builder.append("                    ");
    _builder.append("log.info(\"Created docker-container with name: {}\", containerName);");
    _builder.newLine();
    _builder.append("                ");
    _builder.append("}");
    _builder.newLine();
    _builder.append("            ");
    _builder.append("} catch (IOException e) {");
    _builder.newLine();
    _builder.append("                ");
    _builder.append("e.printStackTrace();");
    _builder.newLine();
    _builder.append("                ");
    _builder.append("log.error(\"Could not create docker-container with name: {}\", containerName);");
    _builder.newLine();
    _builder.append("            ");
    _builder.append("}");
    _builder.newLine();
    _builder.append("        ");
    _builder.append("}");
    _builder.newLine();
    _builder.append("    ");
    _builder.newLine();
    _builder.append("        ");
    _builder.append("private static void startMySQLContainer(String containerName) {");
    _builder.newLine();
    _builder.append("            ");
    _builder.append("try {");
    _builder.newLine();
    _builder.append("                ");
    _builder.append("Process start = Runtime.getRuntime().exec(\"docker start \" + containerName);");
    _builder.newLine();
    _builder.append("                ");
    _builder.append("start.getOutputStream().close();");
    _builder.newLine();
    _builder.append("                ");
    _builder.append("log.info(\"Started docker-container with name: {}\", containerName);");
    _builder.newLine();
    _builder.append("            ");
    _builder.append("} catch (IOException e) {");
    _builder.newLine();
    _builder.append("                ");
    _builder.append("e.printStackTrace();");
    _builder.newLine();
    _builder.append("                ");
    _builder.append("log.error(\"Could\'nt start docker-container with name: {}\", containerName);");
    _builder.newLine();
    _builder.append("            ");
    _builder.append("}");
    _builder.newLine();
    _builder.append("        ");
    _builder.append("}");
    _builder.newLine();
    _builder.newLine();
    _builder.append("    ");
    _builder.append("@Bean");
    _builder.newLine();
    _builder.append("    ");
    _builder.append("public CommandLineRunner loadData(");
    CharSequence _reposAsParams = Helpers.getReposAsParams(backend);
    _builder.append(_reposAsParams, "    ");
    _builder.append(") {");
    _builder.newLineIfNotEmpty();
    _builder.append("        ");
    _builder.append("return (args) -> {");
    _builder.newLine();
    _builder.append("            ");
    int counter = 1;
    _builder.newLineIfNotEmpty();
    {
      EList<Entity> _entities = backend.getEntities();
      for(final Entity entity : _entities) {
        _builder.append("            ");
        String _firstLower = StringExtensions.toFirstLower(entity.getName());
        String _plus = (_firstLower + Integer.valueOf(counter));
        CharSequence _createNewEntity = Helpers.createNewEntity(entity, _plus);
        _builder.append(_createNewEntity, "            ");
        _builder.newLineIfNotEmpty();
        {
          EList<Attribute> _attributes = entity.getAttributes();
          for(final Attribute attribute : _attributes) {
            _builder.append("            ");
            String _firstLower_1 = StringExtensions.toFirstLower(entity.getName());
            String _plus_1 = (_firstLower_1 + Integer.valueOf(counter));
            _builder.append(_plus_1, "            ");
            _builder.append(".set");
            String _firstUpper_3 = StringExtensions.toFirstUpper(attribute.getName());
            _builder.append(_firstUpper_3, "            ");
            _builder.append("(");
            String _randomValueForType = Helpers.getRandomValueForType(attribute, entity);
            _builder.append(_randomValueForType, "            ");
            _builder.append(");");
            _builder.newLineIfNotEmpty();
          }
        }
        _builder.append("            ");
        String _firstLower_2 = StringExtensions.toFirstLower(entity.getName());
        int _plusPlus = counter++;
        String _plus_2 = (_firstLower_2 + Integer.valueOf(_plusPlus));
        CharSequence _saveInRepo = Helpers.saveInRepo(entity, _plus_2);
        _builder.append(_saveInRepo, "            ");
        _builder.newLineIfNotEmpty();
        _builder.append("            ");
        _builder.newLine();
      }
    }
    _builder.append("        ");
    _builder.append("};");
    _builder.newLine();
    _builder.append("    ");
    _builder.append("}");
    _builder.newLine();
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    return _builder;
  }
  
  public CharSequence genWebsecurity(final Backend backend) {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("package ");
    _builder.append(Generator.PACKAGE);
    _builder.append(";");
    _builder.newLineIfNotEmpty();
    _builder.newLine();
    _builder.append("import ");
    _builder.append(Generator.PACKAGE);
    _builder.append(".repositories.AdminRepository;");
    _builder.newLineIfNotEmpty();
    _builder.append("import ");
    _builder.append(Generator.PACKAGE);
    _builder.append(".entities.Admin;");
    _builder.newLineIfNotEmpty();
    _builder.append("import org.springframework.beans.factory.annotation.Autowired;");
    _builder.newLine();
    _builder.append("import org.springframework.context.annotation.Bean;");
    _builder.newLine();
    _builder.append("import org.springframework.context.annotation.Configuration;");
    _builder.newLine();
    _builder.append("import org.springframework.http.HttpMethod;");
    _builder.newLine();
    _builder.append("import org.springframework.security.authentication.AuthenticationManager;");
    _builder.newLine();
    _builder.append("import org.springframework.security.config.BeanIds;");
    _builder.newLine();
    _builder.append("import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;");
    _builder.newLine();
    _builder.append("import org.springframework.security.config.annotation.web.builders.HttpSecurity;");
    _builder.newLine();
    _builder.append("import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;");
    _builder.newLine();
    _builder.append("import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;");
    _builder.newLine();
    _builder.append("import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;");
    _builder.newLine();
    _builder.append("import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;");
    _builder.newLine();
    _builder.newLine();
    _builder.append("@Configuration");
    _builder.newLine();
    _builder.append("@EnableWebSecurity");
    _builder.newLine();
    _builder.append("public class WebSecurityConfig extends WebSecurityConfigurerAdapter {");
    _builder.newLine();
    _builder.append("\t");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("private AdminRepository adminRepository;");
    _builder.newLine();
    _builder.append("\t");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("@Autowired");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("public WebSecurityConfig(AdminRepository adminRepository){");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("this.adminRepository = adminRepository;");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("}");
    _builder.newLine();
    _builder.newLine();
    _builder.append("    ");
    _builder.append("@Override");
    _builder.newLine();
    _builder.append("    ");
    _builder.append("protected void configure(HttpSecurity http) throws Exception {");
    _builder.newLine();
    _builder.append("        ");
    _builder.append("http");
    _builder.newLine();
    _builder.append("                ");
    _builder.append(".csrf().disable() // CSRF is handled by Vaadin: https://vaadin.com/framework/security");
    _builder.newLine();
    _builder.append("                ");
    _builder.append(".exceptionHandling().accessDeniedPage(\"/accessDenied\")");
    _builder.newLine();
    _builder.append("                ");
    _builder.append(".authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint(\"/login\"))");
    _builder.newLine();
    _builder.append("                ");
    _builder.append(".and().logout().logoutSuccessUrl(\"/\")");
    _builder.newLine();
    _builder.append("                ");
    _builder.append(".and()");
    _builder.newLine();
    _builder.append("                ");
    _builder.append(".authorizeRequests()");
    _builder.newLine();
    _builder.append("                ");
    _builder.append("// allow Vaadin URLs and the login URL without authentication");
    _builder.newLine();
    _builder.append("                ");
    _builder.append(".regexMatchers(\"/login.*\", \"/accessDenied\", \"/VAADIN/.*\", \"/favicon.ico\", \"/robots.txt\", \"/manifest.webmanifest\",");
    _builder.newLine();
    _builder.append("                        ");
    _builder.append("\"/sw.js\", \"/offline-page.html\", \"/frontend/.*\", \"/webjars/.*\", \"/frontend-es5/.*\", \"/frontend-es6/.*\").permitAll()");
    _builder.newLine();
    _builder.append("                ");
    _builder.append(".regexMatchers(HttpMethod.POST, \"/\\\\?v-r=.*\").permitAll()");
    _builder.newLine();
    _builder.append("                ");
    _builder.append("// deny any other URL until authenticated");
    _builder.newLine();
    _builder.append("                ");
    _builder.append(".antMatchers(\"/**\").fullyAuthenticated()");
    _builder.newLine();
    _builder.append("            ");
    _builder.append("/*");
    _builder.newLine();
    _builder.append("             ");
    _builder.append("Note that anonymous authentication is enabled by default, therefore;");
    _builder.newLine();
    _builder.append("             ");
    _builder.append("SecurityContextHolder.getContext().getAuthentication().isAuthenticated() always will return true.");
    _builder.newLine();
    _builder.append("             ");
    _builder.append("Look at LoginView.beforeEnter method.");
    _builder.newLine();
    _builder.append("             ");
    _builder.append("more info: https://docs.spring.io/spring-security/site/docs/4.0.x/reference/html/anonymous.html");
    _builder.newLine();
    _builder.append("             ");
    _builder.append("*/");
    _builder.newLine();
    _builder.append("        ");
    _builder.append(";");
    _builder.newLine();
    _builder.append("    ");
    _builder.append("}");
    _builder.newLine();
    _builder.newLine();
    _builder.append("    ");
    _builder.append("@Autowired");
    _builder.newLine();
    _builder.append("    ");
    _builder.append("public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {");
    _builder.newLine();
    _builder.append("        ");
    _builder.append("for(Admin admin:adminRepository.findAll()){");
    _builder.newLine();
    _builder.append("        \t");
    _builder.append("auth.inMemoryAuthentication().passwordEncoder(new BCryptPasswordEncoder())");
    _builder.newLine();
    _builder.append("        \t\t");
    _builder.append(".withUser(admin.getUsername()).password(admin.getPassword()).roles(\"ADMIN\");");
    _builder.newLine();
    _builder.append("        ");
    _builder.append("}");
    _builder.newLine();
    _builder.append("    ");
    _builder.append("}");
    _builder.newLine();
    _builder.newLine();
    _builder.append("    ");
    _builder.append("/**");
    _builder.newLine();
    _builder.append("     ");
    _builder.append("* Expose the AuthenticationManager (to be used in LoginView)");
    _builder.newLine();
    _builder.append("     ");
    _builder.append("*");
    _builder.newLine();
    _builder.append("     ");
    _builder.append("* @return");
    _builder.newLine();
    _builder.append("     ");
    _builder.append("* @throws Exception");
    _builder.newLine();
    _builder.append("     ");
    _builder.append("*/");
    _builder.newLine();
    _builder.append("    ");
    _builder.append("@Bean(name = BeanIds.AUTHENTICATION_MANAGER)");
    _builder.newLine();
    _builder.append("    ");
    _builder.append("@Override");
    _builder.newLine();
    _builder.append("    ");
    _builder.append("public AuthenticationManager authenticationManagerBean() throws Exception {");
    _builder.newLine();
    _builder.append("        ");
    _builder.append("return super.authenticationManagerBean();");
    _builder.newLine();
    _builder.append("    ");
    _builder.append("}");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    return _builder;
  }
  
  public CharSequence genServletInitializer() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("package ");
    _builder.append(Generator.PACKAGE);
    _builder.append(";");
    _builder.newLineIfNotEmpty();
    _builder.newLine();
    _builder.append("import org.springframework.boot.builder.SpringApplicationBuilder;");
    _builder.newLine();
    _builder.append("import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;");
    _builder.newLine();
    _builder.newLine();
    _builder.append("public class ServletInitializer extends SpringBootServletInitializer {");
    _builder.newLine();
    _builder.newLine();
    _builder.append("\t");
    _builder.append("@Override");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("return application.sources(KlostertrophyApplication.class);");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("}");
    _builder.newLine();
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    return _builder;
  }
  
  public CharSequence genMainView(final Backend backend) {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("package ");
    _builder.append(Generator.PACKAGE);
    _builder.append(";");
    _builder.newLineIfNotEmpty();
    _builder.newLine();
    _builder.append("import com.vaadin.flow.component.ClickEvent;");
    _builder.newLine();
    _builder.append("import com.vaadin.flow.component.ComponentEventListener;");
    _builder.newLine();
    _builder.append("import com.vaadin.flow.component.UI;");
    _builder.newLine();
    _builder.append("import com.vaadin.flow.component.button.Button;");
    _builder.newLine();
    _builder.append("import com.vaadin.flow.component.button.ButtonVariant;");
    _builder.newLine();
    _builder.append("import com.vaadin.flow.component.icon.VaadinIcon;");
    _builder.newLine();
    _builder.append("import com.vaadin.flow.component.orderedlayout.HorizontalLayout;");
    _builder.newLine();
    _builder.append("import com.vaadin.flow.component.orderedlayout.VerticalLayout;");
    _builder.newLine();
    _builder.append("import com.vaadin.flow.component.tabs.Tab;");
    _builder.newLine();
    _builder.append("import com.vaadin.flow.component.tabs.Tabs;");
    _builder.newLine();
    _builder.append("import com.vaadin.flow.router.BeforeEnterListener;");
    _builder.newLine();
    _builder.append("import com.vaadin.flow.router.Route;");
    _builder.newLine();
    _builder.append("import com.vaadin.flow.router.RouteAlias;");
    _builder.newLine();
    _builder.append("import com.vaadin.flow.spring.annotation.UIScope;");
    _builder.newLine();
    _builder.append("import org.springframework.beans.factory.annotation.Autowired;");
    _builder.newLine();
    _builder.append("import org.springframework.security.core.context.SecurityContextHolder;");
    _builder.newLine();
    _builder.append("import org.springframework.stereotype.Component;");
    _builder.newLine();
    {
      final Function1<Entity, Boolean> _function = (Entity it) -> {
        return Boolean.valueOf(it.isDisplay());
      };
      Iterable<Entity> _filter = IterableExtensions.<Entity>filter(backend.getEntities(), _function);
      for(final Entity e : _filter) {
        _builder.append("import ");
        _builder.append(Generator.PACKAGE);
        _builder.append(".pages.");
        String _firstUpper = StringExtensions.toFirstUpper(e.getName());
        _builder.append(_firstUpper);
        _builder.append("GridPage;");
        _builder.newLineIfNotEmpty();
      }
    }
    _builder.newLine();
    _builder.newLine();
    _builder.append("import javax.servlet.http.HttpServletRequest;");
    _builder.newLine();
    _builder.append("import java.util.HashMap;");
    _builder.newLine();
    _builder.append("import java.util.Map;");
    _builder.newLine();
    _builder.newLine();
    _builder.append("@Route(\"\")");
    _builder.newLine();
    _builder.append("@RouteAlias(\"main\")");
    _builder.newLine();
    _builder.append("@UIScope");
    _builder.newLine();
    _builder.append("@Component");
    _builder.newLine();
    _builder.append("public class MainView extends VerticalLayout {");
    _builder.newLine();
    _builder.append("\t");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("private HttpServletRequest request;");
    _builder.newLine();
    _builder.append("\t");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("@Autowired");
    _builder.newLine();
    _builder.append("\t    ");
    _builder.append("MainView(HttpServletRequest request, ");
    {
      final Function1<Entity, Boolean> _function_1 = (Entity it) -> {
        return Boolean.valueOf(it.isDisplay());
      };
      Iterable<Entity> _filter_1 = IterableExtensions.<Entity>filter(backend.getEntities(), _function_1);
      boolean _hasElements = false;
      for(final Entity e_1 : _filter_1) {
        if (!_hasElements) {
          _hasElements = true;
        } else {
          _builder.appendImmediate(", ", "\t    ");
        }
        String _firstUpper_1 = StringExtensions.toFirstUpper(e_1.getName());
        _builder.append(_firstUpper_1, "\t    ");
        _builder.append("GridPage ");
        String _name = e_1.getName();
        _builder.append(_name, "\t    ");
        _builder.append("Page");
      }
    }
    _builder.append(") {");
    _builder.newLineIfNotEmpty();
    _builder.append("\t    \t");
    _builder.append("super();");
    _builder.newLine();
    _builder.append("\t    \t");
    _builder.append("this.request = request;");
    _builder.newLine();
    _builder.append("\t    \t");
    _builder.newLine();
    _builder.append("\t    \t");
    _builder.append("Button logout = new Button(VaadinIcon.SIGN_OUT.create());");
    _builder.newLine();
    _builder.append("\t    \t");
    _builder.append("logout.getStyle().set(\"font-size\", \"48px\");");
    _builder.newLine();
    _builder.append("\t    \t");
    _builder.append("logout.setHeight(\"96px\");");
    _builder.newLine();
    _builder.append("\t    \t");
    _builder.append("logout.addThemeVariants(ButtonVariant.LUMO_TERTIARY);");
    _builder.newLine();
    _builder.append("\t    \t");
    _builder.append("logout.addClickListener((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent -> {");
    _builder.newLine();
    _builder.append("\t    \t\t");
    _builder.append("requestLogout();");
    _builder.newLine();
    _builder.append("\t    \t");
    _builder.append("});");
    _builder.newLine();
    _builder.append("\t    \t");
    _builder.newLine();
    _builder.append("\t    \t");
    _builder.append("HorizontalLayout logoutWrapper = new HorizontalLayout();");
    _builder.newLine();
    _builder.append("\t    \t");
    _builder.append("logoutWrapper.add(logout);");
    _builder.newLine();
    _builder.append("\t    \t");
    _builder.append("logoutWrapper.setWidth(\"8%\");");
    _builder.newLine();
    _builder.append("\t    \t");
    _builder.append("logoutWrapper.setJustifyContentMode(JustifyContentMode.END);");
    _builder.newLine();
    {
      final Function1<Entity, Boolean> _function_2 = (Entity it) -> {
        return Boolean.valueOf(it.isDisplay());
      };
      Iterable<Entity> _filter_2 = IterableExtensions.<Entity>filter(backend.getEntities(), _function_2);
      for(final Entity e_2 : _filter_2) {
        _builder.append("\t    \t");
        _builder.append("Tab ");
        String _name_1 = e_2.getName();
        _builder.append(_name_1, "\t    \t");
        _builder.append(" = new Tab(\"");
        String _firstUpper_2 = StringExtensions.toFirstUpper(e_2.getName());
        _builder.append(_firstUpper_2, "\t    \t");
        _builder.append("\");");
        _builder.newLineIfNotEmpty();
        _builder.append("\t    \t");
        _builder.append("        ");
        String _name_2 = e_2.getName();
        _builder.append(_name_2, "\t    \t        ");
        _builder.append(".getStyle().set(\"font-size\", \"48px\");");
        _builder.newLineIfNotEmpty();
      }
    }
    _builder.append("\t    \t");
    _builder.append("Tabs tabs = new Tabs(");
    {
      final Function1<Entity, Boolean> _function_3 = (Entity it) -> {
        return Boolean.valueOf(it.isDisplay());
      };
      Iterable<Entity> _filter_3 = IterableExtensions.<Entity>filter(backend.getEntities(), _function_3);
      boolean _hasElements_1 = false;
      for(final Entity e_3 : _filter_3) {
        if (!_hasElements_1) {
          _hasElements_1 = true;
        } else {
          _builder.appendImmediate(", ", "\t    \t");
        }
        String _name_3 = e_3.getName();
        _builder.append(_name_3, "\t    \t");
      }
    }
    _builder.append(");");
    _builder.newLineIfNotEmpty();
    _builder.append("\t    \t");
    _builder.append("tabs.setSelectedTab(");
    String _name_4 = backend.getEntities().get(0).getName();
    _builder.append(_name_4, "\t    \t");
    _builder.append(");");
    _builder.newLineIfNotEmpty();
    _builder.append("\t    \t");
    _builder.append("tabs.setFlexGrowForEnclosedTabs(1);");
    _builder.newLine();
    _builder.append("\t    \t");
    _builder.append("tabs.setWidthFull();");
    _builder.newLine();
    _builder.append("\t    \t");
    _builder.newLine();
    _builder.append("\t    \t");
    _builder.append("HorizontalLayout tabWrapper = new HorizontalLayout(tabs);");
    _builder.newLine();
    _builder.append("\t    \t");
    _builder.append("tabWrapper.setWidth(\"92%\");");
    _builder.newLine();
    _builder.append("\t    \t");
    _builder.append("tabWrapper.setJustifyContentMode(JustifyContentMode.START);");
    _builder.newLine();
    _builder.append("\t    \t");
    _builder.newLine();
    _builder.append("\t    \t");
    _builder.append("HorizontalLayout bar = new HorizontalLayout(tabWrapper, logoutWrapper);");
    _builder.newLine();
    _builder.append("\t    \t");
    _builder.append("bar.setWidthFull();");
    _builder.newLine();
    _builder.append("\t    \t");
    _builder.newLine();
    _builder.append("\t    \t");
    _builder.append("Map<Tab, VerticalLayout> tabsToPages = new HashMap<>();");
    _builder.newLine();
    {
      final Function1<Entity, Boolean> _function_4 = (Entity it) -> {
        return Boolean.valueOf(it.isDisplay());
      };
      Iterable<Entity> _filter_4 = IterableExtensions.<Entity>filter(backend.getEntities(), _function_4);
      for(final Entity e_4 : _filter_4) {
        _builder.append("\t    \t");
        _builder.append("tabsToPages.put(");
        String _name_5 = e_4.getName();
        _builder.append(_name_5, "\t    \t");
        _builder.append(", ");
        String _name_6 = e_4.getName();
        _builder.append(_name_6, "\t    \t");
        _builder.append("Page);");
        _builder.newLineIfNotEmpty();
      }
    }
    _builder.append("\t    \t");
    _builder.newLine();
    _builder.append("\t    \t");
    _builder.append("tabs.addSelectedChangeListener(event -> {");
    _builder.newLine();
    _builder.append("\t    \t     ");
    _builder.append("removeAll();");
    _builder.newLine();
    _builder.append("\t    \t     ");
    _builder.append("add(bar, tabsToPages.get(tabs.getSelectedTab()));");
    _builder.newLine();
    _builder.append("\t    \t");
    _builder.append("});");
    _builder.newLine();
    _builder.append("\t    \t");
    _builder.newLine();
    _builder.append("\t    \t");
    _builder.append("setSizeFull();");
    _builder.newLine();
    _builder.append("\t    \t");
    _builder.append("add(bar);");
    _builder.newLine();
    _builder.append("\t    \t");
    _builder.newLine();
    _builder.append("\t    \t");
    _builder.append("UI.getCurrent().addBeforeEnterListener((BeforeEnterListener) beforeEnterEvent -> {");
    _builder.newLine();
    _builder.append("\t    \t     ");
    _builder.append("if (beforeEnterEvent.getNavigationTarget() != AccessDeniedView.class && // This is to avoid a");
    _builder.newLine();
    _builder.append("\t    \t     ");
    _builder.append("// loop if DeniedAccessView is the target");
    _builder.newLine();
    _builder.append("\t    \t     ");
    _builder.append("!this.request.isUserInRole(\"ADMIN\")) {");
    _builder.newLine();
    _builder.append("\t    \t     \t");
    _builder.append("beforeEnterEvent.rerouteTo(AccessDeniedView.class);");
    _builder.newLine();
    _builder.append("\t    \t     ");
    _builder.append("}");
    _builder.newLine();
    _builder.append("\t    \t");
    _builder.append("});");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    _builder.newLine();
    _builder.append("void requestLogout() {");
    _builder.newLine();
    _builder.append("        ");
    _builder.append("//https://stackoverflow.com/a/5727444/1572286");
    _builder.newLine();
    _builder.append("        ");
    _builder.append("SecurityContextHolder.clearContext();");
    _builder.newLine();
    _builder.append("        ");
    _builder.append("request.getSession(false).invalidate();");
    _builder.newLine();
    _builder.newLine();
    _builder.append("        ");
    _builder.append("// And this is similar to how logout is handled in Vaadin 8:");
    _builder.newLine();
    _builder.append("        ");
    _builder.append("// https://vaadin.com/docs/v8/framework/articles/HandlingLogout.html");
    _builder.newLine();
    _builder.append("        ");
    _builder.append("UI.getCurrent().getSession().close();");
    _builder.newLine();
    _builder.append("        ");
    _builder.append("UI.getCurrent().getPage().reload();// to redirect user to the login page");
    _builder.newLine();
    _builder.append("    ");
    _builder.append("}");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    return _builder;
  }
  
  public CharSequence genChangeHandler() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("package ");
    _builder.append(Generator.PACKAGE);
    _builder.append(";");
    _builder.newLineIfNotEmpty();
    _builder.newLine();
    _builder.append("public interface ChangeHandler {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("void onChange();");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("}");
    _builder.newLine();
    return _builder;
  }
  
  public CharSequence genAccessDenied() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("package ");
    _builder.append(Generator.PACKAGE);
    _builder.append(";");
    _builder.newLineIfNotEmpty();
    _builder.append("\t\t\t");
    _builder.newLine();
    _builder.append("import com.vaadin.flow.component.formlayout.FormLayout;");
    _builder.newLine();
    _builder.append("import com.vaadin.flow.component.html.Label;");
    _builder.newLine();
    _builder.append("import com.vaadin.flow.component.orderedlayout.VerticalLayout;");
    _builder.newLine();
    _builder.append("import com.vaadin.flow.router.Route;");
    _builder.newLine();
    _builder.newLine();
    _builder.append("@Route(\"accessDenied\")");
    _builder.newLine();
    _builder.append("public class AccessDeniedView extends VerticalLayout {");
    _builder.newLine();
    _builder.append("    ");
    _builder.append("AccessDeniedView() {");
    _builder.newLine();
    _builder.append("        ");
    _builder.append("FormLayout formLayout = new FormLayout();");
    _builder.newLine();
    _builder.append("        ");
    _builder.append("formLayout.add(new Label(\"Access denied!\"));");
    _builder.newLine();
    _builder.append("        ");
    _builder.append("add(formLayout);");
    _builder.newLine();
    _builder.append("    ");
    _builder.append("}");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    return _builder;
  }
  
  public CharSequence genLoginView() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("package ");
    _builder.append(Generator.PACKAGE);
    _builder.append(";");
    _builder.newLineIfNotEmpty();
    _builder.newLine();
    _builder.append("import com.vaadin.flow.component.ClickEvent;");
    _builder.newLine();
    _builder.append("import com.vaadin.flow.component.ComponentEventListener;");
    _builder.newLine();
    _builder.append("import com.vaadin.flow.component.Key;");
    _builder.newLine();
    _builder.append("import com.vaadin.flow.component.KeyDownEvent;");
    _builder.newLine();
    _builder.append("import com.vaadin.flow.component.applayout.AppLayout;");
    _builder.newLine();
    _builder.append("import com.vaadin.flow.component.button.Button;");
    _builder.newLine();
    _builder.append("import com.vaadin.flow.component.formlayout.FormLayout;");
    _builder.newLine();
    _builder.append("import com.vaadin.flow.component.html.Label;");
    _builder.newLine();
    _builder.append("import com.vaadin.flow.component.orderedlayout.FlexComponent;");
    _builder.newLine();
    _builder.append("import com.vaadin.flow.component.orderedlayout.HorizontalLayout;");
    _builder.newLine();
    _builder.append("import com.vaadin.flow.component.orderedlayout.VerticalLayout;");
    _builder.newLine();
    _builder.append("import com.vaadin.flow.component.textfield.PasswordField;");
    _builder.newLine();
    _builder.append("import com.vaadin.flow.component.textfield.TextField;");
    _builder.newLine();
    _builder.append("import com.vaadin.flow.router.BeforeEnterEvent;");
    _builder.newLine();
    _builder.append("import com.vaadin.flow.router.BeforeEnterObserver;");
    _builder.newLine();
    _builder.append("import com.vaadin.flow.router.Route;");
    _builder.newLine();
    _builder.append("import org.apache.commons.lang3.StringUtils;");
    _builder.newLine();
    _builder.append("import org.springframework.beans.factory.annotation.Autowired;");
    _builder.newLine();
    _builder.append("import org.springframework.security.authentication.AnonymousAuthenticationToken;");
    _builder.newLine();
    _builder.append("import org.springframework.security.authentication.AuthenticationManager;");
    _builder.newLine();
    _builder.append("import org.springframework.security.authentication.BadCredentialsException;");
    _builder.newLine();
    _builder.append("import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;");
    _builder.newLine();
    _builder.append("import org.springframework.security.core.Authentication;");
    _builder.newLine();
    _builder.append("import org.springframework.security.core.context.SecurityContext;");
    _builder.newLine();
    _builder.append("import org.springframework.security.core.context.SecurityContextHolder;");
    _builder.newLine();
    _builder.append("import org.springframework.security.web.savedrequest.DefaultSavedRequest;");
    _builder.newLine();
    _builder.newLine();
    _builder.append("import javax.servlet.http.HttpServletRequest;");
    _builder.newLine();
    _builder.append("import javax.servlet.http.HttpSession;");
    _builder.newLine();
    _builder.newLine();
    _builder.append("@Route(\"login\")");
    _builder.newLine();
    _builder.append("public class LoginView extends AppLayout implements BeforeEnterObserver {");
    _builder.newLine();
    _builder.append("\t");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("private final Label label;");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("private final TextField userNameTextField;");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("private final PasswordField passwordField;");
    _builder.newLine();
    _builder.append("\t");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("/**");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("* AuthenticationManager is already exposed in WebSecurityConfig");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("*/");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("@Autowired");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("private AuthenticationManager authManager;");
    _builder.newLine();
    _builder.append("\t");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("@Autowired");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("private HttpServletRequest req;");
    _builder.newLine();
    _builder.append("\t");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("LoginView() {");
    _builder.newLine();
    _builder.append("\t    ");
    _builder.append("label = new Label(\"Bitte anmelden:\");");
    _builder.newLine();
    _builder.append("\t");
    _builder.newLine();
    _builder.append("\t    ");
    _builder.append("userNameTextField = new TextField();");
    _builder.newLine();
    _builder.append("\t    ");
    _builder.append("userNameTextField.setPlaceholder(\"Benutzename\");");
    _builder.newLine();
    _builder.append("\t    ");
    _builder.append("userNameTextField.setWidth(\"90%\");");
    _builder.newLine();
    _builder.append("\t    ");
    _builder.append("//UiUtils.makeFirstInputTextAutoFocus(Collections.singletonList(userNameTextField));");
    _builder.newLine();
    _builder.append("\t");
    _builder.newLine();
    _builder.append("\t    ");
    _builder.append("passwordField = new PasswordField();");
    _builder.newLine();
    _builder.append("\t    ");
    _builder.append("passwordField.setPlaceholder(\"Passwort\");");
    _builder.newLine();
    _builder.append("\t    ");
    _builder.append("passwordField.setWidth(\"90%\");");
    _builder.newLine();
    _builder.append("\t    ");
    _builder.append("passwordField.addKeyDownListener(Key.ENTER, (ComponentEventListener<KeyDownEvent>) keyDownEvent -> authenticateAndNavigate());");
    _builder.newLine();
    _builder.append("\t");
    _builder.newLine();
    _builder.append("\t    ");
    _builder.append("Button submitButton = new Button(\"Anmelden\");");
    _builder.newLine();
    _builder.append("\t    ");
    _builder.append("submitButton.setWidth(\"90%\");");
    _builder.newLine();
    _builder.append("\t    ");
    _builder.append("submitButton.addClickListener((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent -> {");
    _builder.newLine();
    _builder.append("\t        ");
    _builder.append("authenticateAndNavigate();");
    _builder.newLine();
    _builder.append("\t    ");
    _builder.append("});");
    _builder.newLine();
    _builder.append("\t");
    _builder.newLine();
    _builder.append("\t    ");
    _builder.append("FormLayout formLayout = new FormLayout();");
    _builder.newLine();
    _builder.append("\t    ");
    _builder.append("formLayout.add(label, userNameTextField, passwordField, submitButton);");
    _builder.newLine();
    _builder.append("\t");
    _builder.newLine();
    _builder.append("\t    ");
    _builder.append("VerticalLayout verticalLayout = new VerticalLayout(label, userNameTextField, passwordField, submitButton);");
    _builder.newLine();
    _builder.append("\t    ");
    _builder.append("verticalLayout.setHeightFull();");
    _builder.newLine();
    _builder.append("\t    ");
    _builder.append("verticalLayout.setMaxWidth(\"50%\");");
    _builder.newLine();
    _builder.append("\t    ");
    _builder.append("verticalLayout.setAlignItems(FlexComponent.Alignment.CENTER);");
    _builder.newLine();
    _builder.append("\t    ");
    _builder.append("verticalLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);");
    _builder.newLine();
    _builder.append("\t");
    _builder.newLine();
    _builder.append("\t    ");
    _builder.append("HorizontalLayout horizontalLayout = new HorizontalLayout(verticalLayout);");
    _builder.newLine();
    _builder.append("\t    ");
    _builder.append("horizontalLayout.setSizeFull();");
    _builder.newLine();
    _builder.append("\t    ");
    _builder.append("horizontalLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);");
    _builder.newLine();
    _builder.append("\t");
    _builder.newLine();
    _builder.append("\t    ");
    _builder.append("this.setContent(horizontalLayout);");
    _builder.newLine();
    _builder.append("\t  ");
    _builder.append("}");
    _builder.newLine();
    _builder.append("\t");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("private void authenticateAndNavigate() {");
    _builder.newLine();
    _builder.append("\t    ");
    _builder.append("/*");
    _builder.newLine();
    _builder.append("\t    ");
    _builder.append("Set an authenticated user in Spring Security and Spring MVC");
    _builder.newLine();
    _builder.append("\t    ");
    _builder.append("spring-security");
    _builder.newLine();
    _builder.append("\t    ");
    _builder.append("*/");
    _builder.newLine();
    _builder.append("\t    ");
    _builder.append("UsernamePasswordAuthenticationToken authReq = new UsernamePasswordAuthenticationToken(userNameTextField.getValue(), passwordField.getValue());");
    _builder.newLine();
    _builder.append("\t    ");
    _builder.append("try {");
    _builder.newLine();
    _builder.append("\t        ");
    _builder.append("// Set authentication");
    _builder.newLine();
    _builder.append("\t        ");
    _builder.append("Authentication auth = authManager.authenticate(authReq);");
    _builder.newLine();
    _builder.append("\t        ");
    _builder.append("SecurityContext sc = SecurityContextHolder.getContext();");
    _builder.newLine();
    _builder.append("\t        ");
    _builder.append("sc.setAuthentication(auth);");
    _builder.newLine();
    _builder.append("\t");
    _builder.newLine();
    _builder.append("\t        ");
    _builder.append("/*");
    _builder.newLine();
    _builder.append("\t        ");
    _builder.append("Navigate to the requested page:");
    _builder.newLine();
    _builder.append("\t        ");
    _builder.append("This is to redirect a user back to the originally requested URL – after they log in as we are not using");
    _builder.newLine();
    _builder.append("\t        ");
    _builder.append("Spring\'s AuthenticationSuccessHandler.");
    _builder.newLine();
    _builder.append("\t        ");
    _builder.append("*/");
    _builder.newLine();
    _builder.append("\t        ");
    _builder.append("HttpSession session = req.getSession(false);");
    _builder.newLine();
    _builder.append("\t        ");
    _builder.append("DefaultSavedRequest savedRequest = (DefaultSavedRequest) session.getAttribute(\"SPRING_SECURITY_SAVED_REQUEST\");");
    _builder.newLine();
    _builder.append("\t        ");
    _builder.append("//String requestedURI = savedRequest != null ? savedRequest.getRequestURI() : Application.APP_URL;");
    _builder.newLine();
    _builder.append("\t        ");
    _builder.append("String requestedURI = savedRequest != null ? savedRequest.getRequestURI() : \"main\";");
    _builder.newLine();
    _builder.append("\t");
    _builder.newLine();
    _builder.append("\t        ");
    _builder.append("this.getUI().ifPresent(ui -> ui.navigate(StringUtils.removeStart(requestedURI, \"/\")));");
    _builder.newLine();
    _builder.append("\t    ");
    _builder.append("} catch (BadCredentialsException e) {");
    _builder.newLine();
    _builder.append("\t        ");
    _builder.append("label.setText(\"Ungültiger Benutzername oder ungültiges Passwort. Bitte nochmal versuchen.\");");
    _builder.newLine();
    _builder.append("\t    ");
    _builder.append("}");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("}");
    _builder.newLine();
    _builder.append("\t");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("/**");
    _builder.newLine();
    _builder.append("\t ");
    _builder.append("* This is to redirect user to the main URL context if (s)he has already logged in and tries to open /login");
    _builder.newLine();
    _builder.append("\t ");
    _builder.append("*");
    _builder.newLine();
    _builder.append("\t ");
    _builder.append("* @param beforeEnterEvent");
    _builder.newLine();
    _builder.append("\t ");
    _builder.append("*/");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("@Override");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {");
    _builder.newLine();
    _builder.append("\t    ");
    _builder.append("Authentication auth = SecurityContextHolder.getContext().getAuthentication();");
    _builder.newLine();
    _builder.append("\t    ");
    _builder.append("//Anonymous Authentication is enabled in our Spring Security conf");
    _builder.newLine();
    _builder.append("\t    ");
    _builder.append("if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {");
    _builder.newLine();
    _builder.append("\t        ");
    _builder.append("//https://vaadin.com/docs/flow/routing/tutorial-routing-lifecycle.html");
    _builder.newLine();
    _builder.append("\t        ");
    _builder.append("beforeEnterEvent.rerouteTo(\"\");");
    _builder.newLine();
    _builder.append("\t    ");
    _builder.append("}");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("}");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    return _builder;
  }
  
  public CharSequence genEntityClass(final Entity entity) {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("package ");
    _builder.append(Generator.PACKAGE);
    _builder.append(".entities;");
    _builder.newLineIfNotEmpty();
    _builder.newLine();
    _builder.append("import lombok.Getter;");
    _builder.newLine();
    _builder.append("import lombok.NoArgsConstructor;");
    _builder.newLine();
    _builder.append("import lombok.Setter;");
    _builder.newLine();
    _builder.append("import javax.persistence.*;");
    _builder.newLine();
    _builder.append("import java.util.Set;");
    _builder.newLine();
    _builder.newLine();
    _builder.append("@Setter");
    _builder.newLine();
    _builder.append("@Getter");
    _builder.newLine();
    _builder.append("@NoArgsConstructor");
    _builder.newLine();
    _builder.append("@Entity");
    _builder.newLine();
    _builder.append("public class ");
    String _name = entity.getName();
    _builder.append(_name);
    _builder.append("Gen {");
    _builder.newLineIfNotEmpty();
    _builder.append("\t");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("@Id");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("@GeneratedValue");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("@Column(name = \"");
    String _firstLower = StringExtensions.toFirstLower(entity.getName());
    _builder.append(_firstLower, "\t");
    _builder.append("_id\")");
    _builder.newLineIfNotEmpty();
    _builder.append("\t");
    _builder.append("private Long id;");
    _builder.newLine();
    _builder.append("\t");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("// attributes");
    _builder.newLine();
    {
      EList<Attribute> _attributes = entity.getAttributes();
      for(final Attribute attribute : _attributes) {
        {
          if ((attribute instanceof TypeAttribute)) {
            _builder.append("\t");
            _builder.append("private ");
            DataType _type = ((TypeAttribute)attribute).getType();
            _builder.append(_type, "\t");
            _builder.append(" ");
            String _name_1 = ((TypeAttribute)attribute).getName();
            _builder.append(_name_1, "\t");
            _builder.append(";");
            _builder.newLineIfNotEmpty();
          } else {
            if ((attribute instanceof EnumAttribute)) {
              _builder.append("\t");
              _builder.append("@Enumerated(EnumType.STRING)");
              _builder.newLine();
              _builder.append("\t");
              _builder.append("private ");
              String _firstUpper = StringExtensions.toFirstUpper(((EnumAttribute)attribute).getName());
              _builder.append(_firstUpper, "\t");
              _builder.append(" ");
              String _name_2 = ((EnumAttribute)attribute).getName();
              _builder.append(_name_2, "\t");
              _builder.append(";");
              _builder.newLineIfNotEmpty();
            }
          }
        }
      }
    }
    _builder.append("\t");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("// inward relations");
    _builder.newLine();
    {
      EList<Relation> _inwardRelations = entity.getInwardRelations();
      for(final Relation relation : _inwardRelations) {
        {
          RelationType _type_1 = relation.getType();
          boolean _equals = Objects.equal(_type_1, Integer.valueOf(RelationType.ONE_TO_ONE_VALUE));
          if (_equals) {
            _builder.append("\t");
            _builder.append("@OneToOne(mappedBy = \"");
            String _firstLower_1 = StringExtensions.toFirstLower(relation.getStart().getName());
            _builder.append(_firstLower_1, "\t");
            _builder.append("\")");
            _builder.newLineIfNotEmpty();
            _builder.append("\t");
            _builder.append("private ");
            String _firstUpper_1 = StringExtensions.toFirstUpper(relation.getStart().getName());
            _builder.append(_firstUpper_1, "\t");
            _builder.append(" ");
            String _firstLower_2 = StringExtensions.toFirstLower(relation.getStart().getName());
            _builder.append(_firstLower_2, "\t");
            _builder.append(";");
            _builder.newLineIfNotEmpty();
          } else {
            RelationType _type_2 = relation.getType();
            boolean _equals_1 = Objects.equal(_type_2, Integer.valueOf(RelationType.ONE_TO_MANY_VALUE));
            if (_equals_1) {
              _builder.append("\t");
              _builder.append("@ManyToOne");
              _builder.newLine();
              _builder.append("\t");
              _builder.append("@JoinColumn(name = \"");
              String _firstLower_3 = StringExtensions.toFirstLower(relation.getStart().getName());
              _builder.append(_firstLower_3, "\t");
              _builder.append("_id\", nullable = false)");
              _builder.newLineIfNotEmpty();
              _builder.append("\t");
              _builder.append("private ");
              String _firstUpper_2 = StringExtensions.toFirstUpper(relation.getStart().getName());
              _builder.append(_firstUpper_2, "\t");
              _builder.append(" ");
              String _firstLower_4 = StringExtensions.toFirstLower(relation.getStart().getName());
              _builder.append(_firstLower_4, "\t");
              _builder.append(";");
              _builder.newLineIfNotEmpty();
            } else {
              _builder.append("\t");
              _builder.append("@ManyToMany(mappedBy = \"");
              String _firstLower_5 = StringExtensions.toFirstLower(relation.getEnd().getName());
              _builder.append(_firstLower_5, "\t");
              _builder.append("s\")");
              _builder.newLineIfNotEmpty();
              _builder.append("\t");
              _builder.append("private Set<");
              String _firstUpper_3 = StringExtensions.toFirstUpper(relation.getStart().getName());
              _builder.append(_firstUpper_3, "\t");
              _builder.append("> ");
              String _firstLower_6 = StringExtensions.toFirstLower(relation.getStart().getName());
              _builder.append(_firstLower_6, "\t");
              _builder.append("s;");
              _builder.newLineIfNotEmpty();
            }
          }
        }
      }
    }
    _builder.append("\t");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("// outward relations");
    _builder.newLine();
    {
      EList<Relation> _outwardRelations = entity.getOutwardRelations();
      for(final Relation relation_1 : _outwardRelations) {
        {
          RelationType _type_3 = relation_1.getType();
          boolean _equals_2 = Objects.equal(_type_3, Integer.valueOf(RelationType.ONE_TO_ONE_VALUE));
          if (_equals_2) {
            _builder.append("\t");
            _builder.append("@OneToOne(cascade = CascadeType.ALL)");
            _builder.newLine();
            _builder.append("\t");
            _builder.append("@JoinColumn(name = \"");
            String _firstLower_7 = StringExtensions.toFirstLower(relation_1.getEnd().getName());
            _builder.append(_firstLower_7, "\t");
            _builder.append("_id\", referencedColumnName = \"");
            String _firstLower_8 = StringExtensions.toFirstLower(relation_1.getEnd().getName());
            _builder.append(_firstLower_8, "\t");
            _builder.append("_id\")");
            _builder.newLineIfNotEmpty();
            _builder.append("\t");
            _builder.append("private ");
            String _firstUpper_4 = StringExtensions.toFirstUpper(relation_1.getEnd().getName());
            _builder.append(_firstUpper_4, "\t");
            _builder.append(" ");
            String _firstLower_9 = StringExtensions.toFirstLower(relation_1.getEnd().getName());
            _builder.append(_firstLower_9, "\t");
            _builder.append(";");
            _builder.newLineIfNotEmpty();
          } else {
            RelationType _type_4 = relation_1.getType();
            boolean _equals_3 = Objects.equal(_type_4, Integer.valueOf(RelationType.ONE_TO_MANY_VALUE));
            if (_equals_3) {
              _builder.append("\t");
              _builder.append("@OneToMany(mappedBy = \"");
              String _firstLower_10 = StringExtensions.toFirstLower(relation_1.getStart().getName());
              _builder.append(_firstLower_10, "\t");
              _builder.append("\", cascade = CascadeType.ALL)");
              _builder.newLineIfNotEmpty();
              _builder.append("\t");
              _builder.append("private Set<");
              String _firstUpper_5 = StringExtensions.toFirstUpper(relation_1.getEnd().getName());
              _builder.append(_firstUpper_5, "\t");
              _builder.append("> ");
              String _firstLower_11 = StringExtensions.toFirstLower(relation_1.getEnd().getName());
              _builder.append(_firstLower_11, "\t");
              _builder.append("s;");
              _builder.newLineIfNotEmpty();
            } else {
              _builder.append("\t");
              _builder.append("@ManyToMany(cascade = CascadeType.ALL)");
              _builder.newLine();
              _builder.append("\t");
              _builder.append("@JoinTable(");
              _builder.newLine();
              _builder.append("\t");
              _builder.append("name = \"");
              String _firstUpper_6 = StringExtensions.toFirstUpper(relation_1.getStart().getName());
              _builder.append(_firstUpper_6, "\t");
              String _firstUpper_7 = StringExtensions.toFirstUpper(relation_1.getEnd().getName());
              _builder.append(_firstUpper_7, "\t");
              _builder.append("\",");
              _builder.newLineIfNotEmpty();
              _builder.append("\t");
              _builder.append("joinColumns = {@JoinColumn(name = \"");
              String _firstLower_12 = StringExtensions.toFirstLower(relation_1.getStart().getName());
              _builder.append(_firstLower_12, "\t");
              _builder.append("_id\")}, ");
              _builder.newLineIfNotEmpty();
              _builder.append("\t");
              _builder.append("\t\t\t\t");
              _builder.append("inverseJoinColumns = {@JoinColumn(name = \"");
              String _firstLower_13 = StringExtensions.toFirstLower(relation_1.getEnd().getName());
              _builder.append(_firstLower_13, "\t\t\t\t\t");
              _builder.append("_id\")})");
              _builder.newLineIfNotEmpty();
              _builder.append("\t");
              _builder.append("private Set<");
              String _firstUpper_8 = StringExtensions.toFirstUpper(relation_1.getEnd().getName());
              _builder.append(_firstUpper_8, "\t");
              _builder.append("> ");
              String _firstLower_14 = StringExtensions.toFirstLower(relation_1.getEnd().getName());
              _builder.append(_firstLower_14, "\t");
              _builder.append("s;");
              _builder.newLineIfNotEmpty();
            }
          }
        }
      }
    }
    _builder.append("\t");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("// enums");
    _builder.newLine();
    {
      EList<Attribute> _attributes_1 = entity.getAttributes();
      for(final Attribute attribute_1 : _attributes_1) {
        {
          if ((attribute_1 instanceof EnumAttribute)) {
            _builder.append("\t");
            _builder.append("public enum ");
            String _firstUpper_9 = StringExtensions.toFirstUpper(((EnumAttribute)attribute_1).getName());
            _builder.append(_firstUpper_9, "\t");
            _builder.append("{");
            _builder.newLineIfNotEmpty();
            {
              EList<String> _values = ((EnumAttribute)attribute_1).getValues();
              boolean _hasElements = false;
              for(final String value : _values) {
                if (!_hasElements) {
                  _hasElements = true;
                } else {
                  _builder.appendImmediate(", ", "\t\t");
                }
                _builder.append("\t");
                _builder.append("\t");
                String _upperCase = value.toUpperCase();
                _builder.append(_upperCase, "\t\t");
                _builder.newLineIfNotEmpty();
              }
            }
            _builder.append("\t");
            _builder.append("}");
            _builder.newLine();
          }
        }
      }
    }
    _builder.append("}");
    _builder.newLine();
    return _builder;
  }
  
  public CharSequence genTransientEntityClass(final Entity entity) {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("package ");
    _builder.append(Generator.PACKAGE);
    _builder.append(".entities;");
    _builder.newLineIfNotEmpty();
    _builder.newLine();
    _builder.append("import lombok.Getter;");
    _builder.newLine();
    _builder.append("import lombok.NoArgsConstructor;");
    _builder.newLine();
    _builder.append("import lombok.Setter;");
    _builder.newLine();
    _builder.append("import javax.persistence.*;");
    _builder.newLine();
    _builder.append("import java.util.Set;");
    _builder.newLine();
    _builder.newLine();
    _builder.append("@Setter");
    _builder.newLine();
    _builder.append("@Getter");
    _builder.newLine();
    _builder.append("@NoArgsConstructor");
    _builder.newLine();
    _builder.append("public class ");
    String _name = entity.getName();
    _builder.append(_name);
    _builder.append("Gen {");
    _builder.newLineIfNotEmpty();
    _builder.append("\t");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("// attributes");
    _builder.newLine();
    {
      EList<Attribute> _attributes = entity.getAttributes();
      for(final Attribute attribute : _attributes) {
        {
          if ((attribute instanceof TypeAttribute)) {
            _builder.append("\t");
            _builder.append("private ");
            DataType _type = ((TypeAttribute)attribute).getType();
            _builder.append(_type, "\t");
            _builder.append(" ");
            String _name_1 = ((TypeAttribute)attribute).getName();
            _builder.append(_name_1, "\t");
            _builder.append(";");
            _builder.newLineIfNotEmpty();
          } else {
            if ((attribute instanceof EnumAttribute)) {
              _builder.append("\t");
              _builder.append("private ");
              String _firstUpper = StringExtensions.toFirstUpper(((EnumAttribute)attribute).getName());
              _builder.append(_firstUpper, "\t");
              _builder.append(" ");
              String _name_2 = ((EnumAttribute)attribute).getName();
              _builder.append(_name_2, "\t");
              _builder.append(";");
              _builder.newLineIfNotEmpty();
            }
          }
        }
      }
    }
    _builder.append("\t");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("// inward relations");
    _builder.newLine();
    {
      EList<Relation> _inwardRelations = entity.getInwardRelations();
      for(final Relation relation : _inwardRelations) {
        {
          RelationType _type_1 = relation.getType();
          boolean _equals = Objects.equal(_type_1, Integer.valueOf(RelationType.ONE_TO_ONE_VALUE));
          if (_equals) {
            _builder.append("\t");
            _builder.append("private ");
            String _firstUpper_1 = StringExtensions.toFirstUpper(relation.getStart().getName());
            _builder.append(_firstUpper_1, "\t");
            _builder.append(" ");
            String _firstLower = StringExtensions.toFirstLower(relation.getStart().getName());
            _builder.append(_firstLower, "\t");
            _builder.append(";");
            _builder.newLineIfNotEmpty();
          } else {
            RelationType _type_2 = relation.getType();
            boolean _equals_1 = Objects.equal(_type_2, Integer.valueOf(RelationType.ONE_TO_MANY_VALUE));
            if (_equals_1) {
              _builder.append("\t");
              _builder.append("private ");
              String _firstUpper_2 = StringExtensions.toFirstUpper(relation.getStart().getName());
              _builder.append(_firstUpper_2, "\t");
              _builder.append(" ");
              String _firstLower_1 = StringExtensions.toFirstLower(relation.getStart().getName());
              _builder.append(_firstLower_1, "\t");
              _builder.append(";");
              _builder.newLineIfNotEmpty();
            } else {
              _builder.append("\t");
              _builder.append("private Set<");
              String _firstUpper_3 = StringExtensions.toFirstUpper(relation.getStart().getName());
              _builder.append(_firstUpper_3, "\t");
              _builder.append("> ");
              String _firstLower_2 = StringExtensions.toFirstLower(relation.getStart().getName());
              _builder.append(_firstLower_2, "\t");
              _builder.append("s;");
              _builder.newLineIfNotEmpty();
            }
          }
        }
      }
    }
    _builder.append("\t");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("// outward relations");
    _builder.newLine();
    {
      EList<Relation> _outwardRelations = entity.getOutwardRelations();
      for(final Relation relation_1 : _outwardRelations) {
        {
          RelationType _type_3 = relation_1.getType();
          boolean _equals_2 = Objects.equal(_type_3, Integer.valueOf(RelationType.ONE_TO_ONE_VALUE));
          if (_equals_2) {
            _builder.append("\t");
            _builder.append("private ");
            String _firstUpper_4 = StringExtensions.toFirstUpper(relation_1.getEnd().getName());
            _builder.append(_firstUpper_4, "\t");
            _builder.append(" ");
            String _firstLower_3 = StringExtensions.toFirstLower(relation_1.getEnd().getName());
            _builder.append(_firstLower_3, "\t");
            _builder.append(";");
            _builder.newLineIfNotEmpty();
          } else {
            RelationType _type_4 = relation_1.getType();
            boolean _equals_3 = Objects.equal(_type_4, Integer.valueOf(RelationType.ONE_TO_MANY_VALUE));
            if (_equals_3) {
              _builder.append("\t");
              _builder.append("private Set<");
              String _firstUpper_5 = StringExtensions.toFirstUpper(relation_1.getEnd().getName());
              _builder.append(_firstUpper_5, "\t");
              _builder.append("> ");
              String _firstLower_4 = StringExtensions.toFirstLower(relation_1.getEnd().getName());
              _builder.append(_firstLower_4, "\t");
              _builder.append("s;");
              _builder.newLineIfNotEmpty();
            } else {
              _builder.append("\t");
              _builder.append("private Set<");
              String _firstUpper_6 = StringExtensions.toFirstUpper(relation_1.getEnd().getName());
              _builder.append(_firstUpper_6, "\t");
              _builder.append("> ");
              String _firstLower_5 = StringExtensions.toFirstLower(relation_1.getEnd().getName());
              _builder.append(_firstLower_5, "\t");
              _builder.append("s;");
              _builder.newLineIfNotEmpty();
            }
          }
        }
      }
    }
    _builder.append("\t");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("// enums");
    _builder.newLine();
    {
      EList<Attribute> _attributes_1 = entity.getAttributes();
      for(final Attribute attribute_1 : _attributes_1) {
        {
          if ((attribute_1 instanceof EnumAttribute)) {
            _builder.append("\t");
            _builder.append("public enum ");
            String _firstUpper_7 = StringExtensions.toFirstUpper(((EnumAttribute)attribute_1).getName());
            _builder.append(_firstUpper_7, "\t");
            _builder.append("{");
            _builder.newLineIfNotEmpty();
            {
              EList<String> _values = ((EnumAttribute)attribute_1).getValues();
              boolean _hasElements = false;
              for(final String value : _values) {
                if (!_hasElements) {
                  _hasElements = true;
                } else {
                  _builder.appendImmediate(", ", "\t\t");
                }
                _builder.append("\t");
                _builder.append("\t");
                String _upperCase = value.toUpperCase();
                _builder.append(_upperCase, "\t\t");
                _builder.newLineIfNotEmpty();
              }
            }
            _builder.append("\t");
            _builder.append("}");
            _builder.newLine();
          }
        }
      }
    }
    _builder.append("}");
    _builder.newLine();
    return _builder;
  }
  
  public CharSequence genEntityExtensionClass(final Entity entity) {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("package ");
    _builder.append(Generator.PACKAGE);
    _builder.append(".entities;");
    _builder.newLineIfNotEmpty();
    _builder.newLine();
    _builder.append("import ");
    _builder.append(Generator.PACKAGE);
    _builder.append(".entities.");
    String _firstUpper = StringExtensions.toFirstUpper(entity.getName());
    _builder.append(_firstUpper);
    _builder.append("Gen;");
    _builder.newLineIfNotEmpty();
    _builder.newLine();
    _builder.append("public class ");
    String _name = entity.getName();
    _builder.append(_name);
    _builder.append(" extends ");
    String _name_1 = entity.getName();
    _builder.append(_name_1);
    _builder.append("Gen {");
    _builder.newLineIfNotEmpty();
    _builder.append("\t");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    return _builder;
  }
  
  public CharSequence genEntityRepo(final Entity entity) {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("package ");
    _builder.append(Generator.PACKAGE);
    _builder.append(".repositories;");
    _builder.newLineIfNotEmpty();
    _builder.newLine();
    _builder.append("import ");
    _builder.append(Generator.PACKAGE);
    _builder.append(".entities.");
    String _firstUpper = StringExtensions.toFirstUpper(entity.getName());
    _builder.append(_firstUpper);
    _builder.append(";");
    _builder.newLineIfNotEmpty();
    _builder.append("import org.springframework.data.jpa.repository.JpaRepository;");
    _builder.newLine();
    _builder.append("import org.springframework.stereotype.Repository;");
    _builder.newLine();
    _builder.newLine();
    _builder.append("import java.util.List;");
    _builder.newLine();
    _builder.newLine();
    _builder.append("@Repository");
    _builder.newLine();
    _builder.append("public interface ");
    String _firstUpper_1 = StringExtensions.toFirstUpper(entity.getName());
    _builder.append(_firstUpper_1);
    _builder.append("Repository extends JpaRepository<");
    String _firstUpper_2 = StringExtensions.toFirstUpper(entity.getName());
    _builder.append(_firstUpper_2);
    _builder.append(", Long> {");
    _builder.newLineIfNotEmpty();
    _builder.newLine();
    _builder.append("    ");
    _builder.append("List<");
    String _firstUpper_3 = StringExtensions.toFirstUpper(entity.getName());
    _builder.append(_firstUpper_3, "    ");
    _builder.append("> findAll();");
    _builder.newLineIfNotEmpty();
    _builder.newLine();
    _builder.append("    ");
    _builder.append("List<");
    String _firstUpper_4 = StringExtensions.toFirstUpper(entity.getName());
    _builder.append(_firstUpper_4, "    ");
    _builder.append("> findByNameStartsWithIgnoreCase(String name);");
    _builder.newLineIfNotEmpty();
    _builder.newLine();
    _builder.append("    ");
    _builder.append("List<");
    String _firstUpper_5 = StringExtensions.toFirstUpper(entity.getName());
    _builder.append(_firstUpper_5, "    ");
    _builder.append("> findByDoneIs(boolean b);");
    _builder.newLineIfNotEmpty();
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    return _builder;
  }
  
  public CharSequence genEntityGridPage(final Entity entity) {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("\t");
    _builder.append("package ");
    _builder.append(Generator.PACKAGE, "\t");
    _builder.append(".pages;");
    _builder.newLineIfNotEmpty();
    _builder.append("\t");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("import com.vaadin.flow.component.button.Button;");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("import com.vaadin.flow.component.grid.Grid;");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("import com.vaadin.flow.component.grid.GridVariant;");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("import com.vaadin.flow.component.icon.VaadinIcon;");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("import com.vaadin.flow.component.orderedlayout.HorizontalLayout;");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("import com.vaadin.flow.component.orderedlayout.VerticalLayout;");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("import com.vaadin.flow.component.page.Push;");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("import com.vaadin.flow.component.textfield.TextField;");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("import com.vaadin.flow.data.value.ValueChangeMode;");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("import com.vaadin.flow.spring.annotation.UIScope;");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("import ");
    _builder.append(Generator.PACKAGE, "\t");
    _builder.append(".entities.");
    String _firstUpper = StringExtensions.toFirstUpper(entity.getName());
    _builder.append(_firstUpper, "\t");
    _builder.append(";");
    _builder.newLineIfNotEmpty();
    _builder.append("\t");
    _builder.append("import ");
    _builder.append(Generator.PACKAGE, "\t");
    _builder.append(".repositories.");
    String _firstUpper_1 = StringExtensions.toFirstUpper(entity.getName());
    _builder.append(_firstUpper_1, "\t");
    _builder.append("Repository;");
    _builder.newLineIfNotEmpty();
    _builder.append("\t");
    _builder.append("import ");
    _builder.append(Generator.PACKAGE, "\t");
    _builder.append(".details.");
    String _firstUpper_2 = StringExtensions.toFirstUpper(entity.getName());
    _builder.append(_firstUpper_2, "\t");
    _builder.append("Details;");
    _builder.newLineIfNotEmpty();
    _builder.append("\t");
    _builder.append("import ");
    _builder.append(Generator.PACKAGE, "\t");
    _builder.append(".editors.");
    String _firstUpper_3 = StringExtensions.toFirstUpper(entity.getName());
    _builder.append(_firstUpper_3, "\t");
    _builder.append("Editor;");
    _builder.newLineIfNotEmpty();
    _builder.append("\t");
    _builder.append("import org.slf4j.Logger;");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("import org.slf4j.LoggerFactory;");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("import org.springframework.beans.factory.annotation.Autowired;");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("import org.springframework.stereotype.Component;");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("import org.springframework.transaction.annotation.Transactional;");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("import org.springframework.util.StringUtils;");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("// Team Repository entfernt, da nur für Play verwendet");
    _builder.newLine();
    _builder.append("\t");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("@Component");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("@Transactional");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("@UIScope");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("public class ");
    String _firstUpper_4 = StringExtensions.toFirstUpper(entity.getName());
    _builder.append(_firstUpper_4, "\t");
    _builder.append("GridPage extends VerticalLayout {");
    _builder.newLineIfNotEmpty();
    _builder.append("\t");
    _builder.newLine();
    _builder.append("   ");
    _builder.append("private static final long serialVersionUID = -8733687422451328748L;");
    _builder.newLine();
    _builder.append("   ");
    _builder.append("private static final Logger log = LoggerFactory.getLogger(");
    String _firstUpper_5 = StringExtensions.toFirstUpper(entity.getName());
    _builder.append(_firstUpper_5, "   ");
    _builder.append("GridPage.class);");
    _builder.newLineIfNotEmpty();
    _builder.append("\t");
    _builder.newLine();
    _builder.append("\t    ");
    _builder.append("private ");
    String _firstUpper_6 = StringExtensions.toFirstUpper(entity.getName());
    _builder.append(_firstUpper_6, "\t    ");
    _builder.append("Repository ");
    String _name = entity.getName();
    _builder.append(_name, "\t    ");
    _builder.append("Repository;");
    _builder.newLineIfNotEmpty();
    _builder.append("\t    ");
    _builder.append("private Grid<");
    String _firstUpper_7 = StringExtensions.toFirstUpper(entity.getName());
    _builder.append(_firstUpper_7, "\t    ");
    _builder.append("> grid;");
    _builder.newLineIfNotEmpty();
    _builder.append("\t    ");
    _builder.append("private ");
    String _firstUpper_8 = StringExtensions.toFirstUpper(entity.getName());
    _builder.append(_firstUpper_8, "\t    ");
    _builder.append("Editor ");
    String _name_1 = entity.getName();
    _builder.append(_name_1, "\t    ");
    _builder.append("Editor;");
    _builder.newLineIfNotEmpty();
    _builder.append("\t");
    _builder.newLine();
    _builder.append("\t    ");
    _builder.append("private TextField filter;");
    _builder.newLine();
    _builder.append("\t");
    _builder.newLine();
    _builder.append("\t    ");
    _builder.append("private Button evaluate;");
    _builder.newLine();
    _builder.append("\t");
    _builder.newLine();
    _builder.append("\t    ");
    _builder.append("@Autowired");
    _builder.newLine();
    _builder.append("\t    ");
    _builder.append("public ");
    String _firstUpper_9 = StringExtensions.toFirstUpper(entity.getName());
    _builder.append(_firstUpper_9, "\t    ");
    _builder.append("GridPage(");
    String _firstUpper_10 = StringExtensions.toFirstUpper(entity.getName());
    _builder.append(_firstUpper_10, "\t    ");
    _builder.append("Repository ");
    String _name_2 = entity.getName();
    _builder.append(_name_2, "\t    ");
    _builder.append("Repository, ");
    String _firstUpper_11 = StringExtensions.toFirstUpper(entity.getName());
    _builder.append(_firstUpper_11, "\t    ");
    _builder.append("Editor ");
    String _name_3 = entity.getName();
    _builder.append(_name_3, "\t    ");
    _builder.append("Editor) {");
    _builder.newLineIfNotEmpty();
    _builder.append("\t        ");
    _builder.append("super();");
    _builder.newLine();
    _builder.append("\t        ");
    _builder.append("this.");
    String _name_4 = entity.getName();
    _builder.append(_name_4, "\t        ");
    _builder.append("Repository = ");
    String _name_5 = entity.getName();
    _builder.append(_name_5, "\t        ");
    _builder.append("Repository;");
    _builder.newLineIfNotEmpty();
    _builder.append("\t        ");
    _builder.append("this.");
    String _name_6 = entity.getName();
    _builder.append(_name_6, "\t        ");
    _builder.append("Editor = ");
    String _name_7 = entity.getName();
    _builder.append(_name_7, "\t        ");
    _builder.append("Editor;");
    _builder.newLineIfNotEmpty();
    _builder.append("\t");
    _builder.newLine();
    _builder.append("\t        ");
    _builder.append("filter = new TextField();");
    _builder.newLine();
    _builder.append("\t        ");
    _builder.append("HorizontalLayout actions = new HorizontalLayout();");
    _builder.newLine();
    _builder.append("\t");
    _builder.newLine();
    _builder.append("\t        ");
    _builder.append("// grid");
    _builder.newLine();
    _builder.append("\t        ");
    _builder.append("grid = new Grid<>(");
    String _firstUpper_12 = StringExtensions.toFirstUpper(entity.getName());
    _builder.append(_firstUpper_12, "\t        ");
    _builder.append(".class);");
    _builder.newLineIfNotEmpty();
    _builder.append("\t        ");
    _builder.append("grid.setItems(");
    String _name_8 = entity.getName();
    _builder.append(_name_8, "\t        ");
    _builder.append("Repository.findAll());");
    _builder.newLineIfNotEmpty();
    _builder.append("\t        ");
    _builder.append("grid.setMultiSort(true);");
    _builder.newLine();
    _builder.append("\t        ");
    _builder.append("grid.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_NO_ROW_BORDERS,");
    _builder.newLine();
    _builder.append("\t                ");
    _builder.append("GridVariant.LUMO_ROW_STRIPES);");
    _builder.newLine();
    _builder.append("\t        ");
    _builder.append("grid.asSingleSelect().addValueChangeListener(e -> this.");
    String _name_9 = entity.getName();
    _builder.append(_name_9, "\t        ");
    _builder.append("Editor.edit(e.getValue()));");
    _builder.newLineIfNotEmpty();
    _builder.append("\t        ");
    _builder.append("// add Columns");
    _builder.newLine();
    _builder.append("\t        ");
    _builder.append("grid.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_NO_ROW_BORDERS, GridVariant.LUMO_ROW_STRIPES);");
    _builder.newLine();
    _builder.append("\t        ");
    _builder.append("grid.asSingleSelect().addValueChangeListener(e -> this.");
    String _name_10 = entity.getName();
    _builder.append(_name_10, "\t        ");
    _builder.append("Editor.edit(e.getValue()));");
    _builder.newLineIfNotEmpty();
    _builder.append("\t        ");
    _builder.append("//add Columns");
    _builder.newLine();
    _builder.append("\t        ");
    _builder.append("setColumns();");
    _builder.newLine();
    _builder.append("\t");
    _builder.newLine();
    _builder.append("\t        ");
    _builder.append("// actions");
    _builder.newLine();
    _builder.append("\t        ");
    _builder.append("Button addNew = new Button(\"");
    String _firstUpper_13 = StringExtensions.toFirstUpper(entity.getName());
    _builder.append(_firstUpper_13, "\t        ");
    _builder.append(" hinzufügen\", VaadinIcon.PLUS.create());");
    _builder.newLineIfNotEmpty();
    _builder.append("\t        ");
    _builder.append("addNew.addClickListener(e -> this.");
    String _name_11 = entity.getName();
    _builder.append(_name_11, "\t        ");
    _builder.append("Editor.edit(new ");
    String _firstUpper_14 = StringExtensions.toFirstUpper(entity.getName());
    _builder.append(_firstUpper_14, "\t        ");
    _builder.append("()));");
    _builder.newLineIfNotEmpty();
    _builder.append("\t");
    _builder.newLine();
    _builder.append("\t        ");
    _builder.append("// filter");
    _builder.newLine();
    _builder.append("\t        ");
    _builder.append("filter.setPlaceholder(\"Nach Namen filtern\");");
    _builder.newLine();
    _builder.append("\t        ");
    _builder.append("filter.setValueChangeMode(ValueChangeMode.EAGER);");
    _builder.newLine();
    _builder.append("\t        ");
    _builder.append("filter.addValueChangeListener(e -> listValues(e.getValue()));");
    _builder.newLine();
    _builder.append("\t");
    _builder.newLine();
    _builder.append("\t        ");
    _builder.append("// editor");
    _builder.newLine();
    _builder.append("\t        ");
    String _name_12 = entity.getName();
    _builder.append(_name_12, "\t        ");
    _builder.append("Editor.setChangeHandler(() -> {");
    _builder.newLineIfNotEmpty();
    _builder.append("\t            ");
    String _name_13 = entity.getName();
    _builder.append(_name_13, "\t            ");
    _builder.append("Editor.close();");
    _builder.newLineIfNotEmpty();
    _builder.append("\t            ");
    _builder.append("listValues(filter.getValue());");
    _builder.newLine();
    _builder.append("\t        ");
    _builder.append("});");
    _builder.newLine();
    _builder.append("\t");
    _builder.newLine();
    _builder.append("\t");
    _builder.newLine();
    _builder.append("\t        ");
    _builder.append("actions.add(filter, addNew);");
    _builder.newLine();
    _builder.append("\t        ");
    _builder.append("add(actions, grid, this.");
    String _name_14 = entity.getName();
    _builder.append(_name_14, "\t        ");
    _builder.append("Editor);");
    _builder.newLineIfNotEmpty();
    _builder.append("\t        ");
    _builder.append("listValues(null);");
    _builder.newLine();
    _builder.append("\t    ");
    _builder.append("}");
    _builder.newLine();
    _builder.append("\t");
    _builder.newLine();
    _builder.append("\t    ");
    _builder.append("void listValues(String filterText) {");
    _builder.newLine();
    _builder.append("\t        ");
    _builder.append("if (StringUtils.isEmpty(filterText)) {");
    _builder.newLine();
    _builder.append("\t            ");
    _builder.append("grid.setItems(");
    String _name_15 = entity.getName();
    _builder.append(_name_15, "\t            ");
    _builder.append("Repository.findAll());");
    _builder.newLineIfNotEmpty();
    _builder.append("\t        ");
    _builder.append("} else {");
    _builder.newLine();
    _builder.append("\t            ");
    _builder.append("grid.setItems(");
    String _name_16 = entity.getName();
    _builder.append(_name_16, "\t            ");
    _builder.append("Repository.findByNameStartsWithIgnoreCase(filterText));");
    _builder.newLineIfNotEmpty();
    _builder.append("\t        ");
    _builder.append("}");
    _builder.newLine();
    _builder.append("\t    ");
    _builder.append("}");
    _builder.newLine();
    _builder.append("\t");
    _builder.newLine();
    _builder.append("\t    ");
    _builder.append("private void setColumns() {");
    _builder.newLine();
    _builder.append("\t        ");
    _builder.append("// remove unwanted columns");
    _builder.newLine();
    _builder.append("\t        ");
    _builder.append("grid.removeAllColumns();");
    _builder.newLine();
    _builder.append("\t        ");
    _builder.append("// add Columns");
    _builder.newLine();
    {
      EList<Attribute> _attributes = entity.getAttributes();
      for(final Attribute attribute : _attributes) {
        _builder.append("\t        ");
        _builder.append("grid.addColumn(");
        String _firstUpper_15 = StringExtensions.toFirstUpper(entity.getName());
        _builder.append(_firstUpper_15, "\t        ");
        _builder.append("::get");
        String _firstUpper_16 = StringExtensions.toFirstUpper(attribute.getName());
        _builder.append(_firstUpper_16, "\t        ");
        _builder.append(").setHeader(\"");
        String _firstUpper_17 = StringExtensions.toFirstUpper(attribute.getName());
        _builder.append(_firstUpper_17, "\t        ");
        _builder.append("\").setSortable(true);");
        _builder.newLineIfNotEmpty();
      }
    }
    _builder.append("\t");
    _builder.newLine();
    _builder.append("\t");
    _builder.newLine();
    _builder.append("\t        ");
    _builder.append("// add standard-columns");
    _builder.newLine();
    _builder.newLine();
    _builder.append("\t        ");
    _builder.append("grid.addComponentColumn(value -> {");
    _builder.newLine();
    _builder.append("\t            ");
    _builder.append("Button details = new Button(\"Fertig\");");
    _builder.newLine();
    _builder.append("\t            ");
    _builder.append("details.addClassName(\"details\");");
    _builder.newLine();
    _builder.append("\t            ");
    _builder.append("details.addClickListener(e -> {");
    _builder.newLine();
    _builder.append("\t                ");
    _builder.append("var ");
    String _name_17 = entity.getName();
    _builder.append(_name_17, "\t                ");
    _builder.append("Details = new ");
    String _firstUpper_18 = StringExtensions.toFirstUpper(entity.getName());
    _builder.append(_firstUpper_18, "\t                ");
    _builder.append("Details();");
    _builder.newLineIfNotEmpty();
    _builder.append("\t                ");
    String _name_18 = entity.getName();
    _builder.append(_name_18, "\t                ");
    _builder.append("Details.open(value);");
    _builder.newLineIfNotEmpty();
    _builder.append("\t            ");
    _builder.append("});");
    _builder.newLine();
    _builder.append("\t            ");
    _builder.append("if (value.getFinished().isEmpty()) {");
    _builder.newLine();
    _builder.append("\t                ");
    _builder.append("log.info(\"Finished is empty.\");");
    _builder.newLine();
    _builder.append("\t                ");
    _builder.append("details.setEnabled(false);");
    _builder.newLine();
    _builder.append("\t            ");
    _builder.append("} else {");
    _builder.newLine();
    _builder.append("\t                ");
    _builder.append("log.info(\"Finished will be displayed.\");");
    _builder.newLine();
    _builder.append("\t                ");
    _builder.append("details.setEnabled(true);");
    _builder.newLine();
    _builder.append("\t            ");
    _builder.append("}");
    _builder.newLine();
    _builder.append("\t            ");
    _builder.append("return details;");
    _builder.newLine();
    _builder.append("\t        ");
    _builder.append("});");
    _builder.newLine();
    _builder.append("\t");
    _builder.newLine();
    _builder.append("\t        ");
    _builder.append("grid.addComponentColumn(value -> {");
    _builder.newLine();
    _builder.append("\t            ");
    _builder.append("Button edit = new Button(\"Bearbeiten\");");
    _builder.newLine();
    _builder.append("\t            ");
    _builder.append("edit.addClassName(\"edit\");");
    _builder.newLine();
    _builder.append("\t            ");
    _builder.append("edit.addClickListener(e -> {");
    _builder.newLine();
    _builder.append("\t                ");
    String _name_19 = entity.getName();
    _builder.append(_name_19, "\t                ");
    _builder.append("Editor.edit(value);");
    _builder.newLineIfNotEmpty();
    _builder.append("\t            ");
    _builder.append("});");
    _builder.newLine();
    _builder.append("\t            ");
    _builder.append("return edit;");
    _builder.newLine();
    _builder.append("\t        ");
    _builder.append("});");
    _builder.newLine();
    _builder.append("\t");
    _builder.newLine();
    _builder.append("\t    ");
    _builder.append("}");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("}");
    _builder.newLine();
    _builder.append("\t");
    _builder.newLine();
    return _builder;
  }
  
  public CharSequence genEntityEditor(final Entity entity) {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("package ");
    _builder.append(Generator.PACKAGE);
    _builder.append(".editors;");
    _builder.newLineIfNotEmpty();
    _builder.newLine();
    _builder.append("import com.vaadin.flow.component.Key;");
    _builder.newLine();
    _builder.append("import com.vaadin.flow.component.KeyNotifier;");
    _builder.newLine();
    _builder.append("import com.vaadin.flow.component.button.Button;");
    _builder.newLine();
    _builder.append("import com.vaadin.flow.component.dialog.Dialog;");
    _builder.newLine();
    _builder.append("import com.vaadin.flow.component.icon.VaadinIcon;");
    _builder.newLine();
    _builder.append("import com.vaadin.flow.component.orderedlayout.FlexComponent;");
    _builder.newLine();
    _builder.append("import com.vaadin.flow.component.orderedlayout.HorizontalLayout;");
    _builder.newLine();
    _builder.append("import com.vaadin.flow.component.select.Select;");
    _builder.newLine();
    _builder.append("import com.vaadin.flow.component.textfield.TextField;");
    _builder.newLine();
    _builder.append("import com.vaadin.flow.data.binder.Binder;");
    _builder.newLine();
    _builder.append("import com.vaadin.flow.spring.annotation.UIScope;");
    _builder.newLine();
    _builder.append("import ");
    _builder.append(Generator.PACKAGE);
    _builder.append(".entities.");
    String _firstUpper = StringExtensions.toFirstUpper(entity.getName());
    _builder.append(_firstUpper);
    _builder.append(";");
    _builder.newLineIfNotEmpty();
    {
      EList<Relation> _outwardRelations = entity.getOutwardRelations();
      for(final Relation rel : _outwardRelations) {
        _builder.append("import ");
        _builder.append(Generator.PACKAGE);
        _builder.append(".entities.");
        String _firstUpper_1 = StringExtensions.toFirstUpper(rel.getEnd().getName());
        _builder.append(_firstUpper_1);
        _builder.append(";");
        _builder.newLineIfNotEmpty();
      }
    }
    {
      EList<Relation> _inwardRelations = entity.getInwardRelations();
      for(final Relation rel_1 : _inwardRelations) {
        _builder.append("import ");
        _builder.append(Generator.PACKAGE);
        _builder.append(".entities.");
        String _firstUpper_2 = StringExtensions.toFirstUpper(rel_1.getStart().getName());
        _builder.append(_firstUpper_2);
        _builder.append(";");
        _builder.newLineIfNotEmpty();
      }
    }
    _builder.append("import ");
    _builder.append(Generator.PACKAGE);
    _builder.append(".repositories.");
    String _firstUpper_3 = StringExtensions.toFirstUpper(entity.getName());
    _builder.append(_firstUpper_3);
    _builder.append("Repository;");
    _builder.newLineIfNotEmpty();
    {
      EList<Relation> _outwardRelations_1 = entity.getOutwardRelations();
      for(final Relation rel_2 : _outwardRelations_1) {
        _builder.append("import ");
        _builder.append(Generator.PACKAGE);
        _builder.append(".repositories.");
        String _firstUpper_4 = StringExtensions.toFirstUpper(rel_2.getEnd().getName());
        _builder.append(_firstUpper_4);
        _builder.append("Repository;");
        _builder.newLineIfNotEmpty();
      }
    }
    {
      EList<Relation> _inwardRelations_1 = entity.getInwardRelations();
      for(final Relation rel_3 : _inwardRelations_1) {
        _builder.append("import ");
        _builder.append(Generator.PACKAGE);
        _builder.append(".repositories.");
        String _firstUpper_5 = StringExtensions.toFirstUpper(rel_3.getStart().getName());
        _builder.append(_firstUpper_5);
        _builder.append("Repository;");
        _builder.newLineIfNotEmpty();
      }
    }
    _builder.append("import ");
    _builder.append(Generator.PACKAGE);
    _builder.append(".ChangeHandler;");
    _builder.newLineIfNotEmpty();
    _builder.append("import org.springframework.beans.factory.annotation.Autowired;");
    _builder.newLine();
    _builder.append("import org.springframework.stereotype.Component;");
    _builder.newLine();
    _builder.newLine();
    _builder.append("import java.util.ArrayList;");
    _builder.newLine();
    _builder.append("import java.util.EnumSet;");
    _builder.newLine();
    _builder.append("import java.util.List;");
    _builder.newLine();
    _builder.newLine();
    _builder.append("@Component");
    _builder.newLine();
    _builder.append("@UIScope");
    _builder.newLine();
    _builder.append("public class ");
    String _firstUpper_6 = StringExtensions.toFirstUpper(entity.getName());
    _builder.append(_firstUpper_6);
    _builder.append("Editor extends Dialog implements KeyNotifier {");
    _builder.newLineIfNotEmpty();
    _builder.newLine();
    _builder.append("    ");
    _builder.append("private ");
    String _firstUpper_7 = StringExtensions.toFirstUpper(entity.getName());
    _builder.append(_firstUpper_7, "    ");
    _builder.append("Repository ");
    String _firstLower = StringExtensions.toFirstLower(entity.getName());
    _builder.append(_firstLower, "    ");
    _builder.append("Repository;");
    _builder.newLineIfNotEmpty();
    {
      EList<Relation> _outwardRelations_2 = entity.getOutwardRelations();
      for(final Relation rel_4 : _outwardRelations_2) {
        _builder.append("    ");
        _builder.append("private ");
        String _firstUpper_8 = StringExtensions.toFirstUpper(rel_4.getEnd().getName());
        _builder.append(_firstUpper_8, "    ");
        _builder.append("Repository ");
        String _firstLower_1 = StringExtensions.toFirstLower(rel_4.getEnd().getName());
        _builder.append(_firstLower_1, "    ");
        _builder.append("Repository;");
        _builder.newLineIfNotEmpty();
      }
    }
    {
      EList<Relation> _inwardRelations_2 = entity.getInwardRelations();
      for(final Relation rel_5 : _inwardRelations_2) {
        _builder.append("    ");
        _builder.append("private ");
        String _firstUpper_9 = StringExtensions.toFirstUpper(rel_5.getStart().getName());
        _builder.append(_firstUpper_9, "    ");
        _builder.append("Repository ");
        String _firstLower_2 = StringExtensions.toFirstLower(rel_5.getStart().getName());
        _builder.append(_firstLower_2, "    ");
        _builder.append("Repository;");
        _builder.newLineIfNotEmpty();
      }
    }
    _builder.append("    ");
    _builder.append("private ChangeHandler changeHandler;");
    _builder.newLine();
    _builder.append("    ");
    _builder.append("private ");
    String _firstUpper_10 = StringExtensions.toFirstUpper(entity.getName());
    _builder.append(_firstUpper_10, "    ");
    _builder.append(" ");
    String _firstLower_3 = StringExtensions.toFirstLower(entity.getName());
    _builder.append(_firstLower_3, "    ");
    _builder.append(";");
    _builder.newLineIfNotEmpty();
    _builder.newLine();
    _builder.append("    ");
    _builder.append("//buttons");
    _builder.newLine();
    _builder.append("    ");
    _builder.append("Button save = new Button(\"Speichern\", VaadinIcon.CHECK.create());");
    _builder.newLine();
    _builder.append("    ");
    _builder.append("Button cancel = new Button(\"Abbrechen\");");
    _builder.newLine();
    _builder.append("    ");
    _builder.append("Button delete = new Button(\"Löschen\", VaadinIcon.TRASH.create());");
    _builder.newLine();
    _builder.append("    ");
    _builder.append("HorizontalLayout actions = new HorizontalLayout(save, cancel, delete);");
    _builder.newLine();
    _builder.newLine();
    _builder.append("    ");
    _builder.append("//fields to edit");
    _builder.newLine();
    _builder.append("    ");
    _builder.append("TextField name = new TextField(\"");
    String _firstUpper_11 = StringExtensions.toFirstUpper(entity.getName());
    _builder.append(_firstUpper_11, "    ");
    _builder.append("-Name\");");
    _builder.newLineIfNotEmpty();
    {
      Iterable<EnumAttribute> _filter = Iterables.<EnumAttribute>filter(entity.getAttributes(), EnumAttribute.class);
      for(final EnumAttribute e : _filter) {
        _builder.append("\t");
        _builder.append("Select<");
        String _firstUpper_12 = StringExtensions.toFirstUpper(entity.getName());
        _builder.append(_firstUpper_12, "\t");
        _builder.append(".");
        String _firstUpper_13 = StringExtensions.toFirstUpper(e.getName());
        _builder.append(_firstUpper_13, "\t");
        _builder.append("> ");
        String _firstLower_4 = StringExtensions.toFirstLower(e.getName());
        _builder.append(_firstLower_4, "\t");
        _builder.append(" = new Select<>();");
        _builder.newLineIfNotEmpty();
      }
    }
    {
      EList<Relation> _outwardRelations_3 = entity.getOutwardRelations();
      for(final Relation rel_6 : _outwardRelations_3) {
        _builder.append("\t");
        _builder.append("Select<");
        String _firstUpper_14 = StringExtensions.toFirstUpper(rel_6.getEnd().getName());
        _builder.append(_firstUpper_14, "\t");
        _builder.append("> ");
        String _firstLower_5 = StringExtensions.toFirstLower(rel_6.getEnd().getName());
        _builder.append(_firstLower_5, "\t");
        _builder.append(" = new Select<>();");
        _builder.newLineIfNotEmpty();
      }
    }
    {
      EList<Relation> _inwardRelations_3 = entity.getInwardRelations();
      for(final Relation rel_7 : _inwardRelations_3) {
        _builder.append("\t");
        _builder.append("Select<");
        String _firstUpper_15 = StringExtensions.toFirstUpper(rel_7.getStart().getName());
        _builder.append(_firstUpper_15, "\t");
        _builder.append("> ");
        String _firstLower_6 = StringExtensions.toFirstLower(rel_7.getStart().getName());
        _builder.append(_firstLower_6, "\t");
        _builder.append(" = new Select<>();");
        _builder.newLineIfNotEmpty();
      }
    }
    _builder.append("\t");
    _builder.append("HorizontalLayout fields = new HorizontalLayout(name, ");
    {
      Iterable<EnumAttribute> _filter_1 = Iterables.<EnumAttribute>filter(entity.getAttributes(), EnumAttribute.class);
      boolean _hasElements = false;
      for(final EnumAttribute e_1 : _filter_1) {
        if (!_hasElements) {
          _hasElements = true;
        } else {
          _builder.appendImmediate(", ", "\t");
        }
        String _name = e_1.getName();
        _builder.append(_name, "\t");
      }
    }
    _builder.append(");");
    _builder.newLineIfNotEmpty();
    _builder.append("\t");
    _builder.append("Binder<");
    String _firstUpper_16 = StringExtensions.toFirstUpper(entity.getName());
    _builder.append(_firstUpper_16, "\t");
    _builder.append("> binder = new Binder<>(");
    String _firstUpper_17 = StringExtensions.toFirstUpper(entity.getName());
    _builder.append(_firstUpper_17, "\t");
    _builder.append(".class);");
    _builder.newLineIfNotEmpty();
    _builder.newLine();
    _builder.append("    ");
    _builder.append("@Autowired");
    _builder.newLine();
    _builder.append("    ");
    _builder.append("public ");
    String _firstUpper_18 = StringExtensions.toFirstUpper(entity.getName());
    _builder.append(_firstUpper_18, "    ");
    _builder.append("Editor(");
    String _firstUpper_19 = StringExtensions.toFirstUpper(entity.getName());
    _builder.append(_firstUpper_19, "    ");
    _builder.append("Repository ");
    String _firstLower_7 = StringExtensions.toFirstLower(entity.getName());
    _builder.append(_firstLower_7, "    ");
    _builder.append("Repository) {");
    _builder.newLineIfNotEmpty();
    _builder.append("    \t");
    _builder.append("super();");
    _builder.newLine();
    _builder.append("    \t   ");
    _builder.append("this.");
    String _firstLower_8 = StringExtensions.toFirstLower(entity.getName());
    _builder.append(_firstLower_8, "    \t   ");
    _builder.append("Repository = ");
    String _firstLower_9 = StringExtensions.toFirstLower(entity.getName());
    _builder.append(_firstLower_9, "    \t   ");
    _builder.append("Repository;");
    _builder.newLineIfNotEmpty();
    _builder.append("    \t   ");
    _builder.append("add(fields, actions);");
    _builder.newLine();
    _builder.newLine();
    _builder.append("        ");
    _builder.append("// bind using naming convention");
    _builder.newLine();
    _builder.append("        ");
    _builder.append("binder.bindInstanceFields(this);");
    _builder.newLine();
    _builder.newLine();
    _builder.append("        ");
    _builder.append("//actions");
    _builder.newLine();
    _builder.append("        ");
    _builder.append("save.getElement().getThemeList().add(\"primary\");");
    _builder.newLine();
    _builder.append("        ");
    _builder.append("delete.getElement().getThemeList().add(\"error\");");
    _builder.newLine();
    _builder.append("        ");
    _builder.append("addKeyPressListener(Key.ENTER, e -> save());");
    _builder.newLine();
    _builder.append("        ");
    _builder.append("// wire action buttons to save, delete and reset");
    _builder.newLine();
    _builder.append("        ");
    _builder.append("save.addClickListener(e -> save());");
    _builder.newLine();
    _builder.append("        ");
    _builder.append("delete.addClickListener(e -> delete());");
    _builder.newLine();
    _builder.append("        ");
    _builder.append("cancel.addClickListener(e -> changeHandler.onChange());");
    _builder.newLine();
    _builder.newLine();
    _builder.append("        ");
    _builder.append("//fields");
    _builder.newLine();
    {
      Iterable<EnumAttribute> _filter_2 = Iterables.<EnumAttribute>filter(entity.getAttributes(), EnumAttribute.class);
      for(final EnumAttribute e_2 : _filter_2) {
        _builder.append("        ");
        String _name_1 = e_2.getName();
        _builder.append(_name_1, "        ");
        _builder.append(".setLabel(\"");
        String _firstUpper_20 = StringExtensions.toFirstUpper(e_2.getName());
        _builder.append(_firstUpper_20, "        ");
        _builder.append("\");");
        _builder.newLineIfNotEmpty();
        _builder.append("        ");
        String _name_2 = e_2.getName();
        _builder.append(_name_2, "        ");
        _builder.append(".setItemLabelGenerator(");
        String _firstUpper_21 = StringExtensions.toFirstUpper(entity.getName());
        _builder.append(_firstUpper_21, "        ");
        _builder.append(".");
        String _firstUpper_22 = StringExtensions.toFirstUpper(e_2.getName());
        _builder.append(_firstUpper_22, "        ");
        _builder.append("::toString);");
        _builder.newLineIfNotEmpty();
        _builder.append("        ");
        String _name_3 = e_2.getName();
        _builder.append(_name_3, "        ");
        _builder.append(".setItems(new ArrayList<>(EnumSet.allOf(");
        String _firstUpper_23 = StringExtensions.toFirstUpper(entity.getName());
        _builder.append(_firstUpper_23, "        ");
        _builder.append(".");
        String _firstUpper_24 = StringExtensions.toFirstUpper(e_2.getName());
        _builder.append(_firstUpper_24, "        ");
        _builder.append(".class)));");
        _builder.newLineIfNotEmpty();
      }
    }
    {
      EList<Relation> _outwardRelations_4 = entity.getOutwardRelations();
      for(final Relation rel_8 : _outwardRelations_4) {
        _builder.append("        ");
        String _firstLower_10 = StringExtensions.toFirstLower(rel_8.getEnd().getName());
        _builder.append(_firstLower_10, "        ");
        _builder.append(".setLabel(\"");
        String _firstUpper_25 = StringExtensions.toFirstUpper(rel_8.getEnd().getName());
        _builder.append(_firstUpper_25, "        ");
        _builder.append("\");");
        _builder.newLineIfNotEmpty();
        _builder.append("        ");
        _builder.append("List<");
        String _firstUpper_26 = StringExtensions.toFirstUpper(rel_8.getEnd().getName());
        _builder.append(_firstUpper_26, "        ");
        _builder.append("> ");
        String _firstLower_11 = StringExtensions.toFirstLower(rel_8.getEnd().getName());
        _builder.append(_firstLower_11, "        ");
        _builder.append("List = get");
        String _firstUpper_27 = StringExtensions.toFirstUpper(rel_8.getEnd().getName());
        _builder.append(_firstUpper_27, "        ");
        _builder.append("s();");
        _builder.newLineIfNotEmpty();
        _builder.append("        ");
        String _firstLower_12 = StringExtensions.toFirstLower(rel_8.getEnd().getName());
        _builder.append(_firstLower_12, "        ");
        _builder.append(".setItemLabelGenerator(");
        String _firstUpper_28 = StringExtensions.toFirstUpper(rel_8.getEnd().getName());
        _builder.append(_firstUpper_28, "        ");
        _builder.append("::getName);");
        _builder.newLineIfNotEmpty();
        _builder.append("        ");
        String _firstLower_13 = StringExtensions.toFirstLower(rel_8.getEnd().getName());
        _builder.append(_firstLower_13, "        ");
        _builder.append(".setItems(");
        String _firstLower_14 = StringExtensions.toFirstLower(rel_8.getEnd().getName());
        _builder.append(_firstLower_14, "        ");
        _builder.append("List);");
        _builder.newLineIfNotEmpty();
      }
    }
    {
      EList<Relation> _inwardRelations_4 = entity.getInwardRelations();
      for(final Relation rel_9 : _inwardRelations_4) {
        _builder.append("        ");
        String _firstLower_15 = StringExtensions.toFirstLower(rel_9.getStart().getName());
        _builder.append(_firstLower_15, "        ");
        _builder.append(".setLabel(\"");
        String _firstUpper_29 = StringExtensions.toFirstUpper(rel_9.getStart().getName());
        _builder.append(_firstUpper_29, "        ");
        _builder.append("\");");
        _builder.newLineIfNotEmpty();
        _builder.append("        ");
        _builder.append("List<");
        String _firstUpper_30 = StringExtensions.toFirstUpper(rel_9.getStart().getName());
        _builder.append(_firstUpper_30, "        ");
        _builder.append("> ");
        String _firstLower_16 = StringExtensions.toFirstLower(rel_9.getStart().getName());
        _builder.append(_firstLower_16, "        ");
        _builder.append("List = get");
        String _firstUpper_31 = StringExtensions.toFirstUpper(rel_9.getStart().getName());
        _builder.append(_firstUpper_31, "        ");
        _builder.append("s();");
        _builder.newLineIfNotEmpty();
        _builder.append("        ");
        String _firstLower_17 = StringExtensions.toFirstLower(rel_9.getStart().getName());
        _builder.append(_firstLower_17, "        ");
        _builder.append(".setItemLabelGenerator(");
        String _firstUpper_32 = StringExtensions.toFirstUpper(rel_9.getStart().getName());
        _builder.append(_firstUpper_32, "        ");
        _builder.append("::getName);");
        _builder.newLineIfNotEmpty();
        _builder.append("        ");
        String _firstLower_18 = StringExtensions.toFirstLower(rel_9.getStart().getName());
        _builder.append(_firstLower_18, "        ");
        _builder.append(".setItems(");
        String _firstLower_19 = StringExtensions.toFirstLower(rel_9.getStart().getName());
        _builder.append(_firstLower_19, "        ");
        _builder.append("List);");
        _builder.newLineIfNotEmpty();
      }
    }
    _builder.append("        ");
    _builder.newLine();
    _builder.append("        ");
    _builder.append("actions.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);");
    _builder.newLine();
    _builder.append("    ");
    _builder.append("}");
    _builder.newLine();
    _builder.newLine();
    _builder.append("    ");
    _builder.append("public final void edit(");
    String _firstUpper_33 = StringExtensions.toFirstUpper(entity.getName());
    _builder.append(_firstUpper_33, "    ");
    _builder.append(" ");
    String _name_4 = entity.getName();
    _builder.append(_name_4, "    ");
    _builder.append(") {");
    _builder.newLineIfNotEmpty();
    _builder.append("        ");
    _builder.append("if (");
    String _name_5 = entity.getName();
    _builder.append(_name_5, "        ");
    _builder.append(" == null) {");
    _builder.newLineIfNotEmpty();
    _builder.append("            ");
    _builder.append("close();");
    _builder.newLine();
    _builder.append("            ");
    _builder.append("return;");
    _builder.newLine();
    _builder.append("        ");
    _builder.append("}");
    _builder.newLine();
    _builder.newLine();
    _builder.append("        ");
    _builder.append("final boolean persisted = ");
    String _firstLower_20 = StringExtensions.toFirstLower(entity.getName());
    _builder.append(_firstLower_20, "        ");
    _builder.append(".getId() != null;");
    _builder.newLineIfNotEmpty();
    _builder.append("        ");
    _builder.append("if (persisted) {");
    _builder.newLine();
    _builder.append("            ");
    _builder.append("// Find fresh entity for editing");
    _builder.newLine();
    _builder.append("            ");
    _builder.append("this.");
    String _firstLower_21 = StringExtensions.toFirstLower(entity.getName());
    _builder.append(_firstLower_21, "            ");
    _builder.append(" = ");
    String _firstLower_22 = StringExtensions.toFirstLower(entity.getName());
    _builder.append(_firstLower_22, "            ");
    _builder.append("Repository.findById(");
    String _firstLower_23 = StringExtensions.toFirstLower(entity.getName());
    _builder.append(_firstLower_23, "            ");
    _builder.append(".getId()).get();");
    _builder.newLineIfNotEmpty();
    _builder.append("        ");
    _builder.append("}");
    _builder.newLine();
    _builder.append("        ");
    _builder.append("else {");
    _builder.newLine();
    _builder.append("            ");
    _builder.append("this.");
    String _firstLower_24 = StringExtensions.toFirstLower(entity.getName());
    _builder.append(_firstLower_24, "            ");
    _builder.append(" = ");
    String _firstLower_25 = StringExtensions.toFirstLower(entity.getName());
    _builder.append(_firstLower_25, "            ");
    _builder.append(";");
    _builder.newLineIfNotEmpty();
    _builder.append("        ");
    _builder.append("}");
    _builder.newLine();
    _builder.newLine();
    _builder.append("        ");
    _builder.append("this.binder.setBean(this.");
    String _firstLower_26 = StringExtensions.toFirstLower(entity.getName());
    _builder.append(_firstLower_26, "        ");
    _builder.append(");");
    _builder.newLineIfNotEmpty();
    _builder.append("        ");
    _builder.append("open();");
    _builder.newLine();
    _builder.append("        ");
    _builder.append("this.name.focus();");
    _builder.newLine();
    _builder.append("    ");
    _builder.append("}");
    _builder.newLine();
    _builder.newLine();
    _builder.append("    ");
    _builder.append("void save() {");
    _builder.newLine();
    _builder.append("        ");
    _builder.append("if (");
    {
      EList<Attribute> _attributes = entity.getAttributes();
      boolean _hasElements_1 = false;
      for(final Attribute e_3 : _attributes) {
        if (!_hasElements_1) {
          _hasElements_1 = true;
        } else {
          _builder.appendImmediate(" || ", "        ");
        }
        _builder.append("this.");
        String _firstLower_27 = StringExtensions.toFirstLower(entity.getName());
        _builder.append(_firstLower_27, "        ");
        _builder.append(".get");
        String _firstUpper_34 = StringExtensions.toFirstUpper(e_3.getName());
        _builder.append(_firstUpper_34, "        ");
        _builder.append("() == null");
      }
    }
    {
      int _size = IterableExtensions.size(Iterables.<EnumAttribute>filter(entity.getAttributes(), EnumAttribute.class));
      boolean _greaterThan = (_size > 0);
      if (_greaterThan) {
        _builder.append(" || ");
      }
    }
    {
      EList<Relation> _outwardRelations_5 = entity.getOutwardRelations();
      boolean _hasElements_2 = false;
      for(final Relation rel_10 : _outwardRelations_5) {
        if (!_hasElements_2) {
          _hasElements_2 = true;
        } else {
          _builder.appendImmediate(" || ", "        ");
        }
        _builder.append("this.");
        String _firstLower_28 = StringExtensions.toFirstLower(entity.getName());
        _builder.append(_firstLower_28, "        ");
        _builder.append(".get");
        String _firstUpper_35 = StringExtensions.toFirstUpper(rel_10.getEnd().getName());
        _builder.append(_firstUpper_35, "        ");
        _builder.append("() == null");
      }
    }
    {
      int _size_1 = entity.getOutwardRelations().size();
      boolean _greaterThan_1 = (_size_1 > 0);
      if (_greaterThan_1) {
        _builder.append(" || ");
      }
    }
    {
      EList<Relation> _inwardRelations_5 = entity.getInwardRelations();
      boolean _hasElements_3 = false;
      for(final Relation rel_11 : _inwardRelations_5) {
        if (!_hasElements_3) {
          _hasElements_3 = true;
        } else {
          _builder.appendImmediate(" || ", "        ");
        }
        _builder.append("this.");
        String _firstLower_29 = StringExtensions.toFirstLower(entity.getName());
        _builder.append(_firstLower_29, "        ");
        _builder.append(".get");
        String _firstUpper_36 = StringExtensions.toFirstUpper(rel_11.getStart().getName());
        _builder.append(_firstUpper_36, "        ");
        _builder.append("() == null");
      }
    }
    {
      int _size_2 = entity.getInwardRelations().size();
      boolean _greaterThan_2 = (_size_2 > 0);
      if (_greaterThan_2) {
        _builder.append(" || ");
      }
    }
    _builder.append("this.");
    String _firstLower_30 = StringExtensions.toFirstLower(entity.getName());
    _builder.append(_firstLower_30, "        ");
    _builder.append(".getName() == null){");
    _builder.newLineIfNotEmpty();
    _builder.append("            ");
    _builder.append("return;");
    _builder.newLine();
    _builder.append("        ");
    _builder.append("}");
    _builder.newLine();
    _builder.append("        ");
    String _firstLower_31 = StringExtensions.toFirstLower(entity.getName());
    _builder.append(_firstLower_31, "        ");
    _builder.append("Repository.save(this.");
    String _firstLower_32 = StringExtensions.toFirstLower(entity.getName());
    _builder.append(_firstLower_32, "        ");
    _builder.append(");");
    _builder.newLineIfNotEmpty();
    _builder.append("        ");
    _builder.append("this.changeHandler.onChange();");
    _builder.newLine();
    _builder.append("    ");
    _builder.append("}");
    _builder.newLine();
    _builder.newLine();
    _builder.append("\t");
    _builder.append("void delete() {");
    _builder.newLine();
    _builder.append("\t      ");
    String _firstLower_33 = StringExtensions.toFirstLower(entity.getName());
    _builder.append(_firstLower_33, "\t      ");
    _builder.append("Repository.delete(this.");
    String _firstLower_34 = StringExtensions.toFirstLower(entity.getName());
    _builder.append(_firstLower_34, "\t      ");
    _builder.append(");");
    _builder.newLineIfNotEmpty();
    _builder.append("\t      ");
    _builder.append("this.changeHandler.onChange();");
    _builder.newLine();
    _builder.append("\t  ");
    _builder.append("}");
    _builder.newLine();
    _builder.newLine();
    _builder.append("    ");
    _builder.append("public void setChangeHandler(ChangeHandler h) {");
    _builder.newLine();
    _builder.append("        ");
    _builder.append("// ChangeHandler is notified when either save or delete is clicked");
    _builder.newLine();
    _builder.append("        ");
    _builder.append("this.changeHandler = h;");
    _builder.newLine();
    _builder.append("    ");
    _builder.append("}");
    _builder.newLine();
    _builder.append("    ");
    _builder.newLine();
    {
      EList<Relation> _outwardRelations_6 = entity.getOutwardRelations();
      for(final Relation rel_12 : _outwardRelations_6) {
        _builder.append("\t");
        _builder.append("public List<");
        String _firstUpper_37 = StringExtensions.toFirstUpper(rel_12.getEnd().getName());
        _builder.append(_firstUpper_37, "\t");
        _builder.append("> get");
        String _firstUpper_38 = StringExtensions.toFirstUpper(rel_12.getEnd().getName());
        _builder.append(_firstUpper_38, "\t");
        _builder.append("s() {");
        _builder.newLineIfNotEmpty();
        _builder.append("\t");
        _builder.append("\t");
        _builder.append("return ");
        String _firstLower_35 = StringExtensions.toFirstLower(rel_12.getEnd().getName());
        _builder.append(_firstLower_35, "\t\t");
        _builder.append("Repository.findAll();");
        _builder.newLineIfNotEmpty();
        _builder.append("\t");
        _builder.append("}");
        _builder.newLine();
      }
    }
    {
      EList<Relation> _inwardRelations_6 = entity.getInwardRelations();
      for(final Relation rel_13 : _inwardRelations_6) {
        _builder.append("\t");
        _builder.append("public List<");
        String _firstUpper_39 = StringExtensions.toFirstUpper(rel_13.getStart().getName());
        _builder.append(_firstUpper_39, "\t");
        _builder.append("> get");
        String _firstUpper_40 = StringExtensions.toFirstUpper(rel_13.getStart().getName());
        _builder.append(_firstUpper_40, "\t");
        _builder.append("s() {");
        _builder.newLineIfNotEmpty();
        _builder.append("\t");
        _builder.append("\t");
        _builder.append("return ");
        String _firstLower_36 = StringExtensions.toFirstLower(rel_13.getStart().getName());
        _builder.append(_firstLower_36, "\t\t");
        _builder.append("Repository.findAll();");
        _builder.newLineIfNotEmpty();
        _builder.append("\t");
        _builder.append("}");
        _builder.newLine();
      }
    }
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    return _builder;
  }
}
