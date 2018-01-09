package com.github.jenkins.lastchanges.pipeline;

import groovy.lang.Binding;
import hudson.Extension;
import org.jenkinsci.plugins.workflow.cps.CpsScript;
import org.jenkinsci.plugins.workflow.cps.GlobalVariable;

import javax.annotation.Nonnull;


@Extension
public class LastChangesDSL extends GlobalVariable {

    @Nonnull
    @Override
    public String getName() {
        return "LastChanges";
    }

    @Nonnull
    @Override
    public Object getValue(@Nonnull CpsScript cpsScript) throws Exception {
        Binding binding = cpsScript.getBinding();
        Object lastChanges;
        if (binding.hasVariable(getName())) {
            lastChanges = binding.getVariable(getName());
        } else {
            lastChanges = new LastChangesPipelineGlobal(cpsScript);
            binding.setVariable(getName(), lastChanges);
        }
        return lastChanges;
    }
}