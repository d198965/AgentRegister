package com.stk.processer;

import com.stk.annotation.AgentKey;
import com.google.auto.service.AutoService;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

/**
 * Created by zdh on 17/4/26.
 */
@AutoService(Processor.class)
@SupportedAnnotationTypes("com.stk.annotation.AgentKey")
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class AgentKeyProcesser extends AbstractProcessor {
    private Messager messager;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        messager = processingEnv.getMessager();
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        messager.printMessage(Diagnostic.Kind.NOTE, "process...");
        Set<? extends Element> agentKey = roundEnvironment.getElementsAnnotatedWith(AgentKey.class);
        if (agentKey == null || agentKey.size() == 0) {
            messager.printMessage(Diagnostic.Kind.NOTE, "none...");
            return true;
        }
        List<String> keyValueContent = new ArrayList<>();
        for (Element element : agentKey) {
            if (element.getKind() != ElementKind.CLASS) {
                error(element, "invalid kind type", element);
                continue;
            }
            TypeElement variableElement = (TypeElement) element;


            //full class name
            String fqClassName = variableElement.getQualifiedName().toString();
            messager.printMessage(Diagnostic.Kind.NOTE, "fqClassName name " + fqClassName);


            AgentKey agentKeyAnnotation = variableElement.getAnnotation(AgentKey.class);
            String[] keys = agentKeyAnnotation.Keys();
            for (int k = 0; k < keys.length; k++) {
                keyValueContent.add(keys[k] + ";" + fqClassName);
                messager.printMessage(Diagnostic.Kind.NOTE, "key: " + keys[k] + " value: " + element.getSimpleName());
            }
        }
        try {
            if (keyValueContent.size() == 0) {
                return true;
            }
            messager.printMessage(Diagnostic.Kind.NOTE, processingEnv.toString());
            final FileObject fo = processingEnv.getFiler().createResource(
                    StandardLocation.CLASS_OUTPUT, // -d option to javac
                    "",
                    "test");
            String temFilePath = fo.toUri().getPath();
            String outputPath = temFilePath.substring(0, temFilePath.indexOf("build/intermediates/classes"));
            outputPath = outputPath + "src/main/assets/agentkeyvalue/";
            File assertsFile = new File(outputPath);
            if (assertsFile.exists()) {
                if (assertsFile.delete()){
                    messager.printMessage(Diagnostic.Kind.NOTE, "delete assets success");
                }else {
                    messager.printMessage(Diagnostic.Kind.NOTE, "delete assets failed");
                }
            }
            if (!assertsFile.mkdir()) {
                messager.printMessage(Diagnostic.Kind.NOTE, "mkdir assets failed");
                return false;
            }

            File keyValueFile = new File(outputPath + keyValueContent.get(0));
            if (keyValueFile.exists()) {
                keyValueFile.delete();
            }
            keyValueFile.createNewFile();
            String[] valueKeyArrays = new String[keyValueContent.size()];
            writeLineFile(keyValueFile.getAbsolutePath(), keyValueContent.toArray(valueKeyArrays));
            messager.printMessage(Diagnostic.Kind.NOTE, outputPath);
        } catch (Exception ex) {

        }

        return true;
    }

    public void writeLineFile(String filename, String[] content) {
        try {
            FileOutputStream out = new FileOutputStream(filename);
            OutputStreamWriter outWriter = new OutputStreamWriter(out, "UTF-8");
            BufferedWriter bufWrite = new BufferedWriter(outWriter);
            for (int i = 0; i < content.length; i++) {
                bufWrite.write(content[i] + "\r\n");
            }
            bufWrite.close();
            outWriter.close();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("读取" + filename + "出错！");
        }
    }

    private void error(Element element, String message, Object... args) {
        if (args.length > 0) {
            message = String.format(message, args);
        }
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, message, element);
    }
}
