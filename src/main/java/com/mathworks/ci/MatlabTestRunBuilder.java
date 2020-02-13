package com.mathworks.ci;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import javax.annotation.Nonnull;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import com.mathworks.ci.MatlabBuildWrapper.MatabBuildWrapperDescriptor;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Launcher.ProcStarter;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.FormValidation.Kind;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;

public class MatlabTestRunBuilder extends Builder implements SimpleBuildStep {
    
    private int buildResult;
    private EnvVars env;
    private MatlabReleaseInfo matlabRel;
    private CommandConstructUtil cmdUtils;
    private boolean tapChkBx;
    private boolean junitChkBx;
    private boolean coberturaChkBx;
    private boolean stmResultsChkBx;
    private boolean modelCoverageChkBx;
    private boolean pdfReportChkBx;

    @DataBoundConstructor
    public MatlabTestRunBuilder() {


    }


    // Getter and Setters to access local members


    @DataBoundSetter
    public void setTapChkBx(boolean tapChkBx) {
        this.tapChkBx = tapChkBx;
    }

    @DataBoundSetter
    public void setJunitChkBx(boolean junitChkBx) {
        this.junitChkBx = junitChkBx;
    }

    @DataBoundSetter
    public void setCoberturaChkBx(boolean coberturaChkBx) {
        this.coberturaChkBx = coberturaChkBx;
    }
    
    @DataBoundSetter
    public void setStmResultsChkBx(boolean stmResultsChkBx) {
        this.stmResultsChkBx = stmResultsChkBx;
    }
    
    @DataBoundSetter
    public void setModelCoverageChkBx(boolean modelCoverageChkBx) {
        this.modelCoverageChkBx = modelCoverageChkBx;
    }
    
    @DataBoundSetter
    public void setPdfReportChkBx(boolean pdfReportChkBx) {
        this.pdfReportChkBx = pdfReportChkBx;
    }
            
    public boolean getTapChkBx() {
        return tapChkBx;
    }

    public boolean getJunitChkBx() {
        return junitChkBx;
    }

    public boolean getCoberturaChkBx() {
        return coberturaChkBx;
    }

    public boolean getStmResultsChkBx() {
        return stmResultsChkBx;
    }
            
    public boolean getModelCoverageChkBx() {
        return modelCoverageChkBx;
    }
    
    public boolean getPdfReportChkBx() {
        return pdfReportChkBx;
    }
    
    private void setEnv(EnvVars env) {
        this.env = env;
    }
    

    @Extension
    public static class MatlabTestDescriptor extends BuildStepDescriptor<Builder> {


        MatlabReleaseInfo rel;
        

        // Overridden Method used to show the text under build dropdown
        @Override
        public String getDisplayName() {
            return Message.getBuilderDisplayName();
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            save();
            return super.configure(req, formData);
        }

        /*
         * This is to identify which project type in jenkins this should be applicable.(non-Javadoc)
         * 
         * @see hudson.tasks.BuildStepDescriptor#isApplicable(java.lang.Class)
         * 
         * if it returns true then this build step will be applicable for all project type.
         */
        @Override
        public boolean isApplicable(@SuppressWarnings("rawtypes") Class<? extends AbstractProject> jobtype) {
            return true;
        }
        
        
        /*
         * Validation for Test artifact generator checkBoxes
         */

        public FormValidation doCheckCoberturaChkBx(@QueryParameter boolean coberturaChkBx) {
            List<Function<String, FormValidation>> listOfCheckMethods =
                    new ArrayList<Function<String, FormValidation>>();
            if (coberturaChkBx) {
                listOfCheckMethods.add(chkCoberturaSupport);
            }
            return getFirstErrorOrWarning(listOfCheckMethods);
        }

        Function<String, FormValidation> chkCoberturaSupport = (String matlabRoot) -> {
            FilePath matlabRootPath = new FilePath(new File(matlabRoot));
            rel = new MatlabReleaseInfo(matlabRootPath);
            final MatrixPatternResolver resolver = new MatrixPatternResolver(matlabRoot);
            if(!resolver.hasVariablePattern()) {
                try {
                    if (rel.verLessThan(MatlabBuilderConstants.BASE_MATLAB_VERSION_COBERTURA_SUPPORT)) {
                        return FormValidation
                                .warning(Message.getValue("Builder.matlab.cobertura.support.warning"));
                    }
                } catch (MatlabVersionNotFoundException e) {
                    return FormValidation.warning(Message.getValue("Builder.invalid.matlab.root.warning"));
                }
            }
            

            return FormValidation.ok();
        };
        
        public FormValidation doCheckModelCoverageChkBx(@QueryParameter boolean modelCoverageChkBx) {
            List<Function<String, FormValidation>> listOfCheckMethods =
                    new ArrayList<Function<String, FormValidation>>();
            if (modelCoverageChkBx) {
                listOfCheckMethods.add(chkModelCoverageSupport);
            }
            return getFirstErrorOrWarning(listOfCheckMethods);
        }
        
        Function<String, FormValidation> chkModelCoverageSupport = (String matlabRoot) -> {
            FilePath matlabRootPath = new FilePath(new File(matlabRoot));
            rel = new MatlabReleaseInfo(matlabRootPath);
            final MatrixPatternResolver resolver = new MatrixPatternResolver(matlabRoot);
            if(!resolver.hasVariablePattern()) {
                try {
                    if (rel.verLessThan(MatlabBuilderConstants.BASE_MATLAB_VERSION_MODELCOVERAGE_SUPPORT)) {
                        return FormValidation
                                .warning(Message.getValue("Builder.matlab.modelcoverage.support.warning"));
                    }
                } catch (MatlabVersionNotFoundException e) {
                    return FormValidation.warning(Message.getValue("Builder.invalid.matlab.root.warning"));
                }
            }
            
            
            return FormValidation.ok();
        };
        
        public FormValidation doCheckStmResultsChkBx(@QueryParameter boolean stmResultsChkBx) {
            List<Function<String, FormValidation>> listOfCheckMethods =
                    new ArrayList<Function<String, FormValidation>>();
            if (stmResultsChkBx) {
                listOfCheckMethods.add(chkSTMResultsSupport);
            }
            return getFirstErrorOrWarning(listOfCheckMethods);
        }
        
        Function<String, FormValidation> chkSTMResultsSupport = (String matlabRoot) -> {
            FilePath matlabRootPath = new FilePath(new File(matlabRoot));
            rel = new MatlabReleaseInfo(matlabRootPath);
            final MatrixPatternResolver resolver = new MatrixPatternResolver(matlabRoot);
            if(!resolver.hasVariablePattern()) {
                try {
                    if (rel.verLessThan(MatlabBuilderConstants.BASE_MATLAB_VERSION_EXPORTSTMRESULTS_SUPPORT)) {
                        return FormValidation
                                .warning(Message.getValue("Builder.matlab.exportstmresults.support.warning"));
                    }
                } catch (MatlabVersionNotFoundException e) {
                    return FormValidation.warning(Message.getValue("Builder.invalid.matlab.root.warning"));
                }
            }
            
            
            return FormValidation.ok();
        };
        
        public FormValidation getFirstErrorOrWarning(
                List<Function<String, FormValidation>> validations) {
            if (validations == null || validations.isEmpty())
                return FormValidation.ok();
            try {
                final String matlabRoot = Jenkins.getInstance()
                        .getDescriptorByType(MatabBuildWrapperDescriptor.class).getMatlabRootFolder();
                for (Function<String, FormValidation> val : validations) {
                    FormValidation validationResult = val.apply(matlabRoot);
                    if (validationResult.kind.compareTo(Kind.ERROR) == 0
                            || validationResult.kind.compareTo(Kind.WARNING) == 0) {
                        return validationResult;
                    }
                }
            }catch (Exception e) {
                return FormValidation.warning(Message.getValue("Builder.invalid.matlab.root.warning"));
            }
           
            return FormValidation.ok();
        }
    }

    @Override
    public void perform(@Nonnull Run<?, ?> build, @Nonnull FilePath workspace,
            @Nonnull Launcher launcher, @Nonnull TaskListener listener)
            throws InterruptedException, IOException {
        //Set the environment variable specific to the this build
        setEnv(build.getEnvironment(listener));
        
        String matlabRoot = this.env.get("matlabroot");
        cmdUtils = new CommandConstructUtil(launcher, matlabRoot);
        
        //Get node specific matlabroot to get matlab version information
        FilePath nodeSpecificMatlabRoot = new FilePath(launcher.getChannel(),matlabRoot);
        matlabRel = new MatlabReleaseInfo(nodeSpecificMatlabRoot);
        
        // Invoke MATLAB command and transfer output to standard
        // Output Console

        buildResult = execMatlabCommand(workspace, launcher, listener);

        if (buildResult != 0) {
            build.setResult(Result.FAILURE);
        }
    }

    private synchronized int execMatlabCommand(FilePath workspace, Launcher launcher,
            TaskListener listener)
            throws IOException, InterruptedException {
        ProcStarter matlabLauncher;
        try {
            matlabLauncher = launcher.launch().pwd(workspace).envs(this.env);
            if (matlabRel.verLessThan(MatlabBuilderConstants.BASE_MATLAB_VERSION_BATCH_SUPPORT)) {
                ListenerLogDecorator outStream = new ListenerLogDecorator(listener);
                matlabLauncher = matlabLauncher.cmds(cmdUtils.constructDefaultCommandForTestRun(getInputArguments())).stderr(outStream);
            } else {
                matlabLauncher = matlabLauncher.cmds(cmdUtils.constructBatchCommandForTestRun(getInputArguments())).stdout(listener);
            }
                        
            // Copy MATLAB scratch file into the workspace.
            FilePath targetWorkspace = new FilePath(launcher.getChannel(), workspace.getRemote());
            copyMatlabScratchFileInWorkspace(MatlabBuilderConstants.MATLAB_RUNNER_RESOURCE, MatlabBuilderConstants.MATLAB_RUNNER_TARGET_FILE, targetWorkspace);
        } catch (Exception e) {
            listener.getLogger().println(e.getMessage());
            return 1;
        }
        return matlabLauncher.join();
    }
    
    private void copyMatlabScratchFileInWorkspace(String matlabRunnerResourcePath,
            String matlabRunnerTarget, FilePath targetWorkspace)
            throws IOException, InterruptedException {
        final ClassLoader classLoader = getClass().getClassLoader();
        FilePath targetFile =
                new FilePath(targetWorkspace, Message.getValue(matlabRunnerTarget));
        InputStream in = classLoader.getResourceAsStream(matlabRunnerResourcePath);

        targetFile.copyFrom(in);
    }
    
    // Concatenate the input arguments
    private String getInputArguments() {
        String pdfReport = MatlabBuilderConstants.PDF_REPORT + "," + this.getPdfReportChkBx();
        String tapResults = MatlabBuilderConstants.TAP_RESULTS + "," + this.getTapChkBx();
        String junitResults = MatlabBuilderConstants.JUNIT_RESULTS + "," + this.getJunitChkBx();
        String stmResults = MatlabBuilderConstants.STM_RESULTS + "," + this.getStmResultsChkBx();
        String coberturaCodeCoverage = MatlabBuilderConstants.COBERTURA_CODE_COVERAGE + "," + this.getCoberturaChkBx();
        String coberturaModelCoverage = MatlabBuilderConstants.COBERTURA_MODEL_COVERAGE + "," + this.getModelCoverageChkBx();
        
        String inputArgsToMatlabFcn = pdfReport + "," + tapResults + "," + junitResults + ","
                + stmResults + "," + coberturaCodeCoverage + "," + coberturaModelCoverage;
        
        return inputArgsToMatlabFcn;
    }
    

}
