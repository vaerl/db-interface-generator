package generator;

import com.google.common.collect.Iterators;
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
import org.eclipse.xtext.xbase.lib.IteratorExtensions;
import org.eclipse.xtext.xbase.lib.StringExtensions;
import projectMdd.Backend;
import projectMdd.Entity;

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
  public static final String PACKAGE = "de.thm.dbiGenerator.";
  
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
    IFolder repoFolder = this.getAndCreateFolder(project, (Generator.COMPLETE_PATH + "/repos"));
    IFolder pageFolder = this.getAndCreateFolder(project, (Generator.COMPLETE_PATH + "/pages"));
    IFolder gridFolder = this.getAndCreateFolder(project, (Generator.COMPLETE_PATH + "/grids"));
    this.createFile(sourceFolder, "pom.xml", true, this.genPom(backend), progressMonitor);
    this.createFile(resourceFolder, "application.properties", true, this.genApplicationProperties(backend), progressMonitor);
    this.createFile(packageFolder, "MainView.java", true, this.genMainView(backend), progressMonitor);
    this.createFile(packageFolder, "ChangeHandler.java", true, this.genChangeHandler(), progressMonitor);
    this.createFile(packageFolder, "AccessDeniedView.java", true, this.genAccessDenied(), progressMonitor);
    this.createFile(packageFolder, "LoginView.java", true, this.genLoginView(), progressMonitor);
    EList<Entity> _entities = backend.getEntities();
    for (final Entity entity : _entities) {
      {
        String _name = entity.getName();
        String _plus = (_name + ".java");
        this.createFile(entityFolder, _plus, true, this.genEntityExtensionClass(entity), progressMonitor);
        boolean _isTransient = entity.isTransient();
        boolean _not = (!_isTransient);
        if (_not) {
          String _name_1 = entity.getName();
          String _plus_1 = (_name_1 + "Gen.java");
          this.createFile(entityFolder, _plus_1, true, this.genEntityClass(entity), progressMonitor);
          String _name_2 = entity.getName();
          String _plus_2 = (_name_2 + "Repo.java");
          this.createFile(repoFolder, _plus_2, true, this.genEntityRepo(entity), progressMonitor);
          boolean _isDisplay = entity.isDisplay();
          if (_isDisplay) {
            String _name_3 = entity.getName();
            String _plus_3 = (_name_3 + "Page.java");
            this.createFile(pageFolder, _plus_3, true, this.genEntityPage(entity), progressMonitor);
            String _name_4 = entity.getName();
            String _plus_4 = (_name_4 + "Grid.java");
            this.createFile(gridFolder, _plus_4, true, this.genEntityGrid(entity), progressMonitor);
          }
        } else {
          String _name_5 = entity.getName();
          String _plus_5 = (_name_5 + "Gen.java");
          this.createFile(entityFolder, _plus_5, true, this.genTransientEntityClass(entity), progressMonitor);
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
      EList<Entity> _entities = backend.getEntities();
      for(final Entity e : _entities) {
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
      EList<Entity> _entities_1 = backend.getEntities();
      boolean _hasElements = false;
      for(final Entity e_1 : _entities_1) {
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
        _builder.append("page");
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
      EList<Entity> _entities_2 = backend.getEntities();
      for(final Entity e_2 : _entities_2) {
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
      EList<Entity> _entities_3 = backend.getEntities();
      boolean _hasElements_1 = false;
      for(final Entity e_3 : _entities_3) {
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
      EList<Entity> _entities_4 = backend.getEntities();
      for(final Entity e_4 : _entities_4) {
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
    _builder.append("   ");
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
  
  public String genEntityClass(final Entity entity) {
    return "fix implementation";
  }
  
  public CharSequence genTransientEntityClass(final Entity entity) {
    StringConcatenation _builder = new StringConcatenation();
    return _builder;
  }
  
  public CharSequence genEntityExtensionClass(final Entity entity) {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("package ");
    _builder.append(Generator.PACKAGE);
    _builder.append("entities;");
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
    return _builder;
  }
  
  public CharSequence genEntityPage(final Entity entity) {
    StringConcatenation _builder = new StringConcatenation();
    return _builder;
  }
  
  public CharSequence genEntityGrid(final Entity entity) {
    StringConcatenation _builder = new StringConcatenation();
    return _builder;
  }
}
