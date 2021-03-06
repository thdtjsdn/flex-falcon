package org.apache.flex.compiler.internal.codegen.js.goog;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.apache.flex.compiler.codegen.js.IJSPublisher;
import org.apache.flex.compiler.config.Configuration;
import org.apache.flex.compiler.internal.codegen.js.JSPublisher;
import org.apache.flex.compiler.internal.codegen.js.JSSharedData;
import org.apache.flex.compiler.internal.driver.js.goog.JSGoogConfiguration;
import org.apache.flex.compiler.utils.JSClosureCompilerUtil;

import com.google.javascript.jscomp.CheckLevel;
import com.google.javascript.jscomp.ErrorManager;
import com.google.javascript.jscomp.JSError;
import com.google.javascript.jscomp.SourceFile;
import com.google.javascript.jscomp.SourceMap;
import com.google.javascript.jscomp.deps.DepsGenerator;
import com.google.javascript.jscomp.deps.DepsGenerator.InclusionStrategy;

public class JSGoogPublisher extends JSPublisher implements IJSPublisher
{

    public static final String GOOG_INTERMEDIATE_DIR_NAME = "js-intermediate";
    public static final String GOOG_RELEASE_DIR_NAME = "js-release";

    public JSGoogPublisher(Configuration config)
    {
        super(config);
    }

    public File getOutputFolder()
    {
        File outputFolder = new File(configuration.getTargetFileDirectory())
                .getParentFile();
        outputFolder = new File(outputFolder,
                JSGoogPublisher.GOOG_INTERMEDIATE_DIR_NAME);

        return outputFolder;
    }

    public void publish() throws IOException
    {
        final String intermediateDirPath = getOutputFolder().getPath();

        final String projectName = FilenameUtils.getBaseName(configuration
                .getTargetFile());
        final String outputFileName = projectName + "."
                + JSSharedData.OUTPUT_EXTENSION;

        File releaseDir = new File(
                new File(intermediateDirPath).getParentFile(),
                GOOG_RELEASE_DIR_NAME);
        final String releaseDirPath = releaseDir.getPath();
        if (releaseDir.exists())
            org.apache.commons.io.FileUtils.deleteQuietly(releaseDir);
        releaseDir.mkdir();

        final String closureLibDirPath = ((JSGoogConfiguration) configuration)
                .getClosureLib();
        final String closureGoogSrcLibDirPath = closureLibDirPath
                + "/closure/goog/";
        final String closureGoogTgtLibDirPath = intermediateDirPath
                + "/library/closure/goog";
        final String closureTPSrcLibDirPath = closureLibDirPath
                + "/third_party/closure/goog/";
        final String closureTPTgtLibDirPath = intermediateDirPath
                + "/library/third_party/closure/goog";
        final String vanillaSDKSrcLibDirPath = ((JSGoogConfiguration) configuration)
                .getVanillaSDKLib();
        final String vanillaSDKTgtLibDirPath = intermediateDirPath
                + "/VanillaSDK";

        final String depsSrcFilePath = intermediateDirPath
                + "/library/closure/goog/deps.js";
        final String depsTgtFilePath = intermediateDirPath + "/deps.js";
        final String projectIntermediateJSFilePath = intermediateDirPath
                + File.separator + outputFileName;
        final String projectReleaseJSFilePath = releaseDirPath + File.separator
                + outputFileName;

        appendExportSymbol(projectIntermediateJSFilePath, projectName);

        copyFile(vanillaSDKSrcLibDirPath, vanillaSDKTgtLibDirPath);

        List<SourceFile> inputs = new ArrayList<SourceFile>();
        Collection<File> files = org.apache.commons.io.FileUtils.listFiles(
                new File(intermediateDirPath),
                new RegexFileFilter("^.*(\\.js)"),
                DirectoryFileFilter.DIRECTORY);
        for (File file : files)
        {
            inputs.add(SourceFile.fromFile(file));
        }

        copyFile(closureGoogSrcLibDirPath, closureGoogTgtLibDirPath);
        copyFile(closureTPSrcLibDirPath, closureTPTgtLibDirPath);

        File srcDeps = new File(depsSrcFilePath);

        final List<SourceFile> deps = new ArrayList<SourceFile>();
        deps.add(SourceFile.fromFile(srcDeps));

        ErrorManager errorManager = new JSGoogErrorManager();
        DepsGenerator depsGenerator = new DepsGenerator(deps, inputs,
                InclusionStrategy.ALWAYS, closureGoogTgtLibDirPath,
                errorManager);
        writeFile(depsTgtFilePath, depsGenerator.computeDependencyCalls(),
                false);

        org.apache.commons.io.FileUtils.deleteQuietly(srcDeps);
        org.apache.commons.io.FileUtils.moveFile(new File(depsTgtFilePath),
                srcDeps);

        writeHTML("intermediate", projectName, intermediateDirPath);
        writeHTML("release", projectName, releaseDirPath);

        ArrayList<String> optionList = new ArrayList<String>();

        files = org.apache.commons.io.FileUtils.listFiles(new File(
                intermediateDirPath), new RegexFileFilter("^.*(\\.js)"),
                DirectoryFileFilter.DIRECTORY);
        for (File file : files)
        {
            optionList.add("--js=" + file.getCanonicalPath());
        }

        optionList.add("--closure_entry_point=" + projectName);
        optionList.add("--only_closure_dependencies");
        optionList.add("--compilation_level=ADVANCED_OPTIMIZATIONS");
        optionList.add("--js_output_file=" + projectReleaseJSFilePath);
        optionList.add("--output_manifest=" + releaseDirPath + File.separator
                + "manifest.txt");
        optionList.add("--create_source_map=" + projectReleaseJSFilePath
                + ".map");
        optionList.add("--source_map_format=" + SourceMap.Format.V3);

        String[] options = (String[]) optionList.toArray(new String[0]);

        JSClosureCompilerUtil.run(options);

        appendSourceMapLocation(projectReleaseJSFilePath);

        System.out.println("The project '" + projectName
                + "' has been successfully compiled and optimized.");
    }

    private void appendExportSymbol(String path, String projectName)
            throws IOException
    {
        StringBuilder appendString = new StringBuilder();
        appendString
                .append("\n\n// Ensures the symbol will be visible after compiler renaming.\n");
        appendString.append("goog.exportSymbol('");
        appendString.append(projectName);
        appendString.append("', ");
        appendString.append(projectName);
        appendString.append(");\n");
        writeFile(path, appendString.toString(), true);
    }

    private void appendSourceMapLocation(String path) throws IOException
    {
        StringBuilder appendString = new StringBuilder();
        appendString.append("\n//@ sourceMappingURL=./Example.js.map");
        writeFile(path, appendString.toString(), true);
    }

    private void copyFile(String srcPath, String tgtPath) throws IOException
    {
        File srcFile = new File(srcPath);
        if (srcFile.isDirectory())
            org.apache.commons.io.FileUtils.copyDirectory(srcFile, new File(
                    tgtPath));
        else
            org.apache.commons.io.FileUtils
                    .copyFile(srcFile, new File(tgtPath));
    }

    private void writeHTML(String type, String projectName, String dirPath)
            throws IOException
    {
        StringBuilder htmlFile = new StringBuilder();
        htmlFile.append("<!DOCTYPE html>\n");
        htmlFile.append("<html>\n");
        htmlFile.append("<head>\n");
        htmlFile.append("\t<meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge,chrome=1\">\n");
        htmlFile.append("\t<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">\n");

        if (type == "intermediate")
        {
            htmlFile.append("\t<script type=\"text/javascript\" src=\"./library/closure/goog/base.js\"></script>\n");
            htmlFile.append("\t<script type=\"text/javascript\">\n");
            htmlFile.append("\t\tgoog.require(\"");
            htmlFile.append(projectName);
            htmlFile.append("\");\n");
            htmlFile.append("\t</script>\n");
        }
        else
        {
            htmlFile.append("\t<script type=\"text/javascript\" src=\"./");
            htmlFile.append(projectName);
            htmlFile.append(".js\"></script>\n");
        }

        htmlFile.append("</head>\n");
        htmlFile.append("<body>\n");
        htmlFile.append("\t<script type=\"text/javascript\">\n");
        htmlFile.append("\t\tnew ");
        htmlFile.append(projectName);
        htmlFile.append("();\n");
        htmlFile.append("\t</script>\n");
        htmlFile.append("</body>\n");
        htmlFile.append("</html>");

        writeFile(dirPath + File.separator + "index.html", htmlFile.toString(),
                false);
    }

    private void writeFile(String path, String content, boolean append)
            throws IOException
    {
        File tgtFile = new File(path);

        if (!tgtFile.exists())
            tgtFile.createNewFile();

        FileWriter fw = new FileWriter(tgtFile, append);
        fw.write(content);
        fw.close();
    }

    public class JSGoogErrorManager implements ErrorManager
    {
        @Override
        public void setTypedPercent(double arg0)
        {
        }

        @Override
        public void report(CheckLevel arg0, JSError arg1)
        {
        }

        @Override
        public JSError[] getWarnings()
        {
            return null;
        }

        @Override
        public int getWarningCount()
        {
            return 0;
        }

        @Override
        public double getTypedPercent()
        {
            return 0;
        }

        @Override
        public JSError[] getErrors()
        {
            return null;
        }

        @Override
        public int getErrorCount()
        {
            return 0;
        }

        @Override
        public void generateReport()
        {
        }
    }
}
