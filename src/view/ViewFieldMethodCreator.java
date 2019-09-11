package view;

import com.intellij.codeInsight.actions.ReformatCodeProcessor;
import com.intellij.openapi.command.WriteCommandAction.Simple;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import entity.Element;
import utils.Util;

import java.util.List;

/**
 * 用来生成我们需要的控件注入与事件注入代码
 */
public class ViewFieldMethodCreator extends Simple {

    private FindViewByIdDialog mDialog;
    private Editor mEditor;
    private PsiFile mFile;
    private Project mProject;
    private PsiClass mClass;
    private List<Element> elementList;
    private PsiElementFactory mFactory;

    public ViewFieldMethodCreator(FindViewByIdDialog dialog, Editor editor, PsiFile psiFile, PsiClass psiClass, String command, List<Element> elements, String selectedText) {
        super(psiClass.getProject(), command);
        mDialog = dialog;
        mEditor = editor;
        mFile = psiFile;
        mProject = psiClass.getProject();
        mClass = psiClass;
        elementList = elements;
        // 获取Factory
        mFactory = JavaPsiFacade.getElementFactory(mProject);
    }

    /**
     * 单独用一个线程来生成代码
     *
     * @throws Throwable
     */
    @Override
    protected void run() throws Throwable {
        //生成属性
        generateFields();
        //生成点击监听方法
        generateOnClickListener();
        //重新mainActivity 把内容插入到MainActivity。Java文件里面
        //1找到对应的项目
        JavaCodeStyleManager styleManager=JavaCodeStyleManager.getInstance(mProject);
        //2连接psi文件  (mFile 光标所在可编辑界面文件）
        styleManager.optimizeImports(mFile);

        //3 就是2中的psi关联上mClass
        styleManager.shortenClassReferences(mClass);

        //4执行
        ReformatCodeProcessor reformatCodeProcessor = new ReformatCodeProcessor(mProject, mClass.getContainingFile(), null, false);
        //执行代码
        reformatCodeProcessor.run();

        Util.showPopupBalloon(mEditor,"生成成功",5);

    }


    /**
     * 生成属性-->  @BindView(R.id.text)
     * TextView text;
     */
    private void generateFields() {
        for (Element element : elementList) {

            /**
             *方法注入
             * 执行下面代码就写到AS的编辑页面里面
             * isCreateFiled是否勾选属性
             */
            if (element.isCreateFiled()) {
                //拼接字符串
                StringBuffer stringBuffer = new StringBuffer();
//            @BindView(R.id.text)
                stringBuffer.append("@BindView(" + element.getFullID() + ")\n");
//         private   TextView text;
                stringBuffer.append("private " + element.getName() + " " + element.getFieldName() + ";");


                //核心代码注入到MainActivity里面

                //public interface PsiField extends PsiJvmMember, PsiVariable, PsiDocCommentOwner, JvmField {
                PsiField fieldFromText = mFactory.createFieldFromText(stringBuffer.toString(), mClass);

                mClass.add(fieldFromText);
            }


        }

    }


    /**
     * 生成点击监听方法
     *
     * @OnClick(R.id.text) public void onViewClicked(TextView tvText) {
     * * <p>
     * * }
     */
    private void generateOnClickListener() {
        for (Element mElement : elementList) {
            //是否勾选点击监听方法
            boolean createClickMethod = mElement.isCreateClickMethod();
            if (createClickMethod) {
                //getClickMethodName这个方法主要text_name的转换成驼峰命令的 textName
                //name为方法名
                String name = getClickMethodName(mElement) + "Clicked";
                //获取mClass（相当mainActivity是否有name这方法，有就返回方法集合）
                PsiMethod[] methodsByName = mClass.findMethodsByName(name, true);

                //看activity里面是否有这方法，没有就创建，
                if (methodsByName.length <= 0) {
                    //写监听方法
                    createClickMethod(name, mElement);
                }


            }
        }

    }

    /**
     * 写方法 核心代码
     *
     * @param name     ------》@OnClick(R.id.text) public void onViewClicked(TextView tvText) {
     *                 <p>
     *                 }
     * @param mElement
     */
    private void createClickMethod(String name, Element mElement) {
        StringBuffer sb = new StringBuffer();
//        @OnClick(R.id.text)
        sb.append("@OnClick(" + mElement.getFullID() + ")\n");
//        public void onViewClicked(TextView tvText) {
        sb.append("public void on" + name + "(" + mElement.getName() + " " + getClickMethodName(mElement)+ ")" + "{\n");

//        }
        sb.append("}");

        PsiMethod methodFromText = mFactory.createMethodFromText(sb.toString(), mClass);
        mClass.add(methodFromText);

    }

    /**
     * 把tv_text的命名转换成 tvText
     *
     * @param mElement
     * @return
     */
    private String getClickMethodName(Element mElement) {
        String[] name = mElement.getId().split("_");
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < name.length; i++) {
            if (i == 0) {
                sb.append(name[i]);
            } else {
                sb.append(Util.firstToUpperCase(name[i]));
            }
        }

        return sb.toString();

    }



}
