package view;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.BusyObject;
import com.intellij.psi.PsiFile;

public class text extends WriteCommandAction.Simple {


    protected text(Project project, PsiFile... files) {
        super(project, files);
    }


    //线程
    @Override
    protected void run() throws Throwable {

    }


}
