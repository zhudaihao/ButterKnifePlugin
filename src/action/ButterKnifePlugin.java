package action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiUtilBase;
import com.intellij.psi.xml.XmlFile;
import entity.Element;
import org.apache.http.util.TextUtils;
import org.jetbrains.annotations.NotNull;
import utils.Util;
import com.intellij.openapi.ui.Messages;
import view.FindViewByIdDialog;

import java.util.ArrayList;
import java.util.List;

public class ButterKnifePlugin extends AnAction {
    private String xmlFileName;
    private FindViewByIdDialog mDialog;

    @Override
    public void actionPerformed(@NotNull AnActionEvent anActionEvent) {

        //获取使用插件的工程对象
        Project project = anActionEvent.getProject();
        //获取编译区对象（光标所在额页面）
        Editor editor = anActionEvent.getData(PlatformDataKeys.EDITOR);
        //通过编辑区对象获取，光标所选中的对象
        SelectionModel selectionModel = editor.getSelectionModel();
        //光标选中文本
        xmlFileName = selectionModel.getSelectedText();

        //如果光标没有选中就对光标所在行处理
        if (TextUtils.isEmpty(xmlFileName)) {
            //获取当前光标所在行的名称---》activity_main
            xmlFileName = getCurrentLayout(editor);
            //如果光标没有放到布局行，弹输入框 让用户自己输入
            if (TextUtils.isEmpty(xmlFileName)) {
                //输入对话框
                xmlFileName = Messages.showInputDialog(project, "请输入layout名字", "未输入", Messages.getInformationIcon());

                //如果用户没有输入，就弹个提示
                Util.showPopupBalloon(editor, "用户没有输入layout名字", 5);

            }

        }

        //代码运行到这 说明通过光标，已获取到布局名称
        //解析结构化文件--》xml，类。。（代码块的文件都可以解析）
        //使用系统工具-->FilenameIndex;工具会从从你传入的第一个参数对象找 xmlFileName+".xml"的文件名
        //参数（搜索的对象，搜索条件，搜索范围） allScope(project)--->对project对象所有进行搜索
        PsiFile[] psiFiles = FilenameIndex.getFilesByName(project, xmlFileName + ".xml", GlobalSearchScope.allScope(project));
        if (psiFiles.length <= 0) {
            Util.showPopupBalloon(editor, "未找到选中的布局文件", 5);
            return;
        }

        //找这文件就去获取对应的psi文件
        XmlFile xmlFile = (XmlFile) psiFiles[0];
        //解析XML文件---》系统工具解析
        //解析后的信息保存到一个对象Element对象---》一个element相当布局里面的一个控件 例如TextView
        List<Element> list = new ArrayList<>();

        //从布局解析后的信息保存到list里面
        List<Element> elementList = Util.getIDsFromLayout(xmlFile, list);

        //生成UI
        if (elementList.size()>0){
            //返回光标所在编辑文件--》例如相当MainActivity文件
            PsiFile psiFile = PsiUtilBase.getPsiFileInEditor(editor, project);
            PsiClass psiClass=Util.getTargetClass(editor,psiFile);
            mDialog =new  FindViewByIdDialog(editor,project,psiFile,psiClass,elementList,xmlFileName);
            //调用显示
            mDialog.showDialog();
        }



    }

    /**
     * 获取光标所在行 的布局名称
     *
     * @param editor
     * @return
     */
    private String getCurrentLayout(Editor editor) {

        //获取editor的document模型
        Document document = editor.getDocument();
        //获取create模型
        CaretModel caretModel = editor.getCaretModel();
        //获取光标的位置（offset==当前编译页最顶部文本到广标所在位置的字母总和）
        int offset = caretModel.getOffset();

        //获取光标所在行号
        int lineNumber = document.getLineNumber(offset);
        //获取光标所在行号的开始位置
        int lineStartOffset = document.getLineStartOffset(lineNumber);
        //获取光标所在行号的结束位置
        int lineEndOffset = document.getLineEndOffset(lineNumber);

        //获取光标所在行是文本
        String layoutName = document.getText(new TextRange(lineStartOffset, lineEndOffset));
        String layout = "R.layout.";

        if (!TextUtils.isEmpty(layoutName) && layoutName.contains(layout)) {
            int start = layoutName.indexOf(layout) + layout.length();
            int end = layoutName.indexOf(")", start);

            return layoutName.substring(start, end);

        }


        return null;
    }


}
