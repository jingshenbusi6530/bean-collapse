package com.aruisi.beancollapse;

import com.intellij.lang.ASTNode;
import com.intellij.lang.folding.FoldingBuilderEx;
import com.intellij.lang.folding.FoldingDescriptor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.FoldingGroup;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class BeanCollapseFoldingBuilder extends FoldingBuilderEx {
    @Override
    public FoldingDescriptor @NotNull [] buildFoldRegions(@NotNull PsiElement root, @NotNull Document document, boolean quick) {
        List<FoldingDescriptor> descriptors = new ArrayList<>();
        
        // 获取所有字段
        Collection<PsiField> fields = PsiTreeUtil.findChildrenOfType(root, PsiField.class);
        
        // 找到第一个和最后一个注解的位置
        TextRange fullRange = null;
        StringBuilder placeholderText = new StringBuilder();
        
        for (PsiField field : fields) {
            PsiAnnotation[] annotations = field.getModifierList().getAnnotations();
            for (PsiAnnotation annotation : annotations) {
                String qualifiedName = annotation.getQualifiedName();
                if (qualifiedName != null && (
                    qualifiedName.equals("javax.annotation.Resource") ||
                    qualifiedName.equals("org.springframework.beans.factory.annotation.Autowired"))) {
                    
                    // 添加到占位符文本
                    if (placeholderText.length() > 0) {
                        placeholderText.append("\n");
                    }
                    placeholderText.append("@ ").append(field.getName())
                                 .append(": ").append(field.getType().getPresentableText());
                    
                    // 更新范围
                    TextRange currentRange = new TextRange(
                        annotation.getTextRange().getStartOffset(),
                        field.getTextRange().getEndOffset()
                    );
                    
                    if (fullRange == null) {
                        fullRange = currentRange;
                    } else {
                        fullRange = new TextRange(
                            Math.min(fullRange.getStartOffset(), currentRange.getStartOffset()),
                            Math.max(fullRange.getEndOffset(), currentRange.getEndOffset())
                        );
                    }
                    
                    break;
                }
            }
        }
        
        // 如果找到了需要折叠的区域
        if (fullRange != null) {
            FoldingGroup group = FoldingGroup.newGroup("bean_injection");
            descriptors.add(new FoldingDescriptor(
                root.getNode(),
                fullRange,
                group) {
                @Override
                public String getPlaceholderText() {
                    return placeholderText.toString();
                }
            });
        }
        
        return descriptors.toArray(new FoldingDescriptor[0]);
    }

    @Nullable
    @Override
    public String getPlaceholderText(@NotNull ASTNode node) {
        return "...";  // 这个方法不会被调用，因为我们在FoldingDescriptor中重写了getPlaceholderText
    }

    @Override
    public boolean isCollapsedByDefault(@NotNull ASTNode node) {
        return true;
    }
} 