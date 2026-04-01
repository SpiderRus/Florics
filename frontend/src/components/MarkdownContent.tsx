import React from 'react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';

interface MarkdownContentProps {
    content: string;
    className?: string;
}

const MarkdownContent: React.FC<MarkdownContentProps> = ({ content, className = '' }) => {
    return (
        <div className={`markdown-content ${className}`}>
            <ReactMarkdown
                remarkPlugins={[remarkGfm]}
                components={{
                    h1: ({ node, ...props }) => <h1 className="markdown-h1" {...props} />,
                    h2: ({ node, ...props }) => <h2 className="markdown-h2" {...props} />,
                    h3: ({ node, ...props }) => <h3 className="markdown-h3" {...props} />,
                    p: ({ node, ...props }) => <p className="markdown-paragraph" {...props} />,
                    ul: ({ node, ...props }) => <ul className="markdown-list" {...props} />,
                    ol: ({ node, ...props }) => <ol className="markdown-ordered-list" {...props} />,
                    li: ({ node, ...props }) => <li className="markdown-list-item" {...props} />,
                    a: ({ node, ...props }) => (
                        <a
                            className="markdown-link"
                            target="_blank"
                            rel="noopener noreferrer"
                            {...props}
                        />
                    ),
                    strong: ({ node, ...props }) => <strong className="markdown-bold" {...props} />,
                    em: ({ node, ...props }) => <em className="markdown-italic" {...props} />,
                    code: ({ node, className, children, ...props }: any) => {
                        const isInline = !className || !className.includes('language-');
                        if (isInline) {
                            return <code className="markdown-inline-code" {...props}>{children}</code>;
                        }
                        return (
                            <div className="markdown-code-block">
                                <code {...props}>{children}</code>
                            </div>
                        );
                    },
                }}
            >
                {content}
            </ReactMarkdown>
        </div>
    );
};

export default MarkdownContent;
