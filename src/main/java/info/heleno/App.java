/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package info.heleno;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.text.Normalizer;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.apache.maven.model.Build;
import org.apache.maven.model.Contributor;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Developer;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.io.DefaultModelWriter;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;

/**
 *
 * @author helenocampos
 */
public class App {

    static String jacocoVersion = "0.8.2";

    public static void main(String[] args) {
        if (args.length > 0) {
            String projectPath = args[0];
            Model parentPom = readPom(projectPath);
            List<Model> modules = new LinkedList<>();
            List<String> modulesFoldersName = parentPom.getModules();
            String groupIdModule = parentPom.getGroupId();
            if (modulesFoldersName != null && !modulesFoldersName.isEmpty()) {
                for (String moduleFolderName : modulesFoldersName) {
                    File moduleFolder = new File(projectPath, moduleFolderName);
                    if (moduleFolder.exists()) {
                        Model modulePom = readPom(moduleFolder.getAbsolutePath());
                        if (modulePom != null) {
                            modules.add(modulePom);
                            if (modulePom.getGroupId() != null) {
                                groupIdModule = modulePom.getGroupId();
                            }

                        }
                    }
                }

                File aggregatorFolder = new File(projectPath, "aggregator");
                aggregatorFolder.mkdir();
                Model aggregatorModel = getAggregatorModelPom(parentPom.getArtifactId(),
                        parentPom.getVersion(), parentPom.getGroupId(), modules, groupIdModule);
                writePom(aggregatorModel, aggregatorFolder.getAbsolutePath());

                Plugin plugin = new Plugin();
                plugin.setGroupId("org.jacoco");
                plugin.setArtifactId("jacoco-maven-plugin");
                plugin.setVersion("0.8.2");
                PluginExecution pluginExecution = new PluginExecution();
                pluginExecution.addGoal("prepare-agent");
                plugin.addExecution(pluginExecution);

                Build build = parentPom.getBuild();
                if(build == null){
                    build = new Build();
                }
                build.setPlugins(removePlugin("jacoco-maven-plugin", build.getPlugins()));
                build.addPlugin(plugin);
                parentPom.setBuild(build);
                if (!parentPom.getModules().contains("aggregator")) {
                    parentPom.addModule("aggregator");
                }

                writePom(parentPom, projectPath);

            }
        }

    }

    private static List<Plugin> removePlugin(String artifactId, List<Plugin> plugins) {
        Iterator<Plugin> pluginsIterator = plugins.iterator();
        while (pluginsIterator.hasNext()) {
            Plugin plugin = pluginsIterator.next();
            if (plugin.getArtifactId().equals(artifactId)) {
                pluginsIterator.remove();
            }
        }
        return plugins;
    }

    public static Model getAggregatorModelPom(String parentId, String parentVersion, String parentGroupId, List<Model> dependencies, String moduleGroupId) {
        Model pomModel = new Model();
        pomModel.setModelVersion("4.0.0");
        Parent parent = new Parent();
        parent.setArtifactId(parentId);
        parent.setGroupId(parentGroupId);
        parent.setVersion(parentVersion);
        pomModel.setParent(parent);
        for (Model dependency : dependencies) {
            Dependency dep = new Dependency();
            if (dependency.getGroupId() != null) {
                dep.setGroupId(dependency.getGroupId());
            } else {
                dep.setGroupId(moduleGroupId);
            }
            if (dependency.getArtifactId() != null) {
                dep.setArtifactId(dependency.getArtifactId());
            } else {

            }
            if (dependency.getVersion() != null) {
                dep.setVersion(dependency.getVersion());
            } else {
                dep.setVersion("${project.version}");
            }

            pomModel.addDependency(dep);
        }
        pomModel.setArtifactId("aggregator");
        pomModel.setGroupId(moduleGroupId);
        pomModel.setVersion(parentVersion);

        Plugin plugin = new Plugin();
        plugin.setGroupId("org.jacoco");
        plugin.setArtifactId("jacoco-maven-plugin");
        plugin.setVersion("0.8.2");
        PluginExecution pluginExecution = new PluginExecution();
        pluginExecution.setId("report-aggregate");
        pluginExecution.setPhase("test");
        pluginExecution.addGoal("report-aggregate");
        plugin.addExecution(pluginExecution);

        Build build = new Build();
        build.addPlugin(plugin);

        pomModel.setBuild(build);

        return pomModel;
    }

    public static Model readPom(String projectFolder) {
        MavenXpp3Reader reader = new MavenXpp3Reader();
        File newProjectDir = new File(projectFolder);
        Model pomModel = null;
        if (newProjectDir.exists()) {
            File pom = new File(newProjectDir, "pom.xml");
            if (pom.exists()) {
                try {
                    pomModel = reader.read(new FileReader(pom));
                } catch (Exception ex) {
//                    Logger.getLogger(MyMojo.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        return pomModel;
    }

    public static void writePom(Model model, String projectFolder) {
        File newProjectDir = new File(projectFolder);
        if (newProjectDir.exists()) {
            File pom = new File(newProjectDir, "pom.xml");

            try {
                DefaultModelWriter writer = new DefaultModelWriter();
                OutputStream output = new FileOutputStream(pom);
                replaceSpecialCharacters(model);
                writer.write(output, null, model);
                output.close();
            } catch (FileNotFoundException ex) {
//                    Logger.getLogger(MyMojo.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
//                    Logger.getLogger(MyMojo.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
    }

    private static void replaceSpecialCharacters(Model model) {
        List<Contributor> contributors = model.getContributors();
        if (contributors != null) {
            for (Contributor contributor : contributors) {
                String name = contributor.getName();
                if (name != null) {
                    contributor.setName(Normalizer.normalize(name, Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}", ""));
                }
            }
        }
        List<Developer> developers = model.getDevelopers();
        if (developers != null) {
            for (Developer developer : developers) {
                String name = developer.getName();
                if (name != null) {
                    developer.setName(Normalizer.normalize(name, Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}", ""));
                }
            }
        }
    }
}
