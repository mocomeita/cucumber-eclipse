package cucumber.eclipse.editor.editors;

import gherkin.formatter.Formatter;
import gherkin.formatter.model.Background;
import gherkin.formatter.model.BasicStatement;
import gherkin.formatter.model.DescribedStatement;
import gherkin.formatter.model.Examples;
import gherkin.formatter.model.Feature;
import gherkin.formatter.model.Scenario;
import gherkin.formatter.model.ScenarioOutline;
import gherkin.formatter.model.Step;
import gherkin.lexer.LexingError;
import gherkin.parser.ParseError;
import gherkin.parser.Parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;

public class GherkinModel {

	protected static class PositionedElement {
		private BasicStatement statement;
		private int endOffset = -1;
		private IDocument document;

		public PositionedElement(IDocument doc, BasicStatement stmt) {
			this.statement = stmt;
			this.document = doc;
		}

		private static int getDocumentLine(int line) {
			// numbering in document is 0-based;
			return line - 1;
		}

		public void setEndLine(int lineNo) {
			try {
				endOffset = document.getLineOffset(getDocumentLine(lineNo))
						+ document.getLineLength(getDocumentLine(lineNo));
			} catch (BadLocationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		public BasicStatement getStatement() {
			return statement;
		}

		public Position toPosition() throws BadLocationException {
			int offset = document.getLineOffset(getDocumentLine(statement
					.getLine()));
			if (endOffset == -1) {
				endOffset = offset
						+ document.getLineLength(getDocumentLine(statement
								.getLine()));
			}

			return new Position(offset, endOffset - offset);
		}
	}
	
	private List<PositionedElement> elements = new ArrayList<PositionedElement>();
	
	public List<Position> getFoldRanges() {
		List<Position> foldRanges = new ArrayList<Position>();
		for (PositionedElement element : elements) {
			try {
				foldRanges.add(element.toPosition());
			} catch (BadLocationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return foldRanges;
	}

	public void updateFromDocument(final IDocument document) {
		elements.clear();
		
		Parser p = new Parser(new Formatter() {

			private Stack<PositionedElement> stack = new Stack<PositionedElement>();

			@Override
			public void uri(String arg0) {
			}

			@Override
			public void syntaxError(String arg0, String arg1,
					List<String> arg2, String arg3, Integer arg4) {
			}

			@Override
			public void step(Step arg0) {
				stack.peek().setEndLine(arg0.getLineRange().getLast());
			}

			private boolean isStepContainer(BasicStatement stmt) {
				return stmt instanceof Scenario
						|| stmt instanceof ScenarioOutline
						|| stmt instanceof Background;
			}

			@Override
			public void scenarioOutline(ScenarioOutline arg0) {
				handleStepContainer(arg0);
			}

			private void handleStepContainer(DescribedStatement stmt) {
				if (isStepContainer(stack.peek().getStatement())) {
					stack.pop();
				}
				stack.push(newPositionedElement(stmt));
			}

			@Override
			public void scenario(Scenario arg0) {
				handleStepContainer(arg0);
			}

			@Override
			public void feature(Feature arg0) {
				stack.push(newPositionedElement(arg0));
			}

			@Override
			public void examples(Examples arg0) {
				int lastLine = getLastExamplesLine(arg0);
				newPositionedElement(arg0).setEndLine(lastLine);
				stack.peek().setEndLine(lastLine);
			}

			@Override
			public void eof() {
				while (!stack.isEmpty()) {
					stack.pop().setEndLine(document.getNumberOfLines());
				}
			}

			@Override
			public void done() {
			}

			@Override
			public void close() {
			}

			@Override
			public void background(Background arg0) {
				handleStepContainer(arg0);
			}
			
			private PositionedElement newPositionedElement(DescribedStatement stmt) {
				PositionedElement element = new PositionedElement(document, stmt);
				elements.add(element);
				return element;
			}

			private int getLastExamplesLine(Examples examples) {
				int lastline = examples.getLineRange().getLast();
				if (!examples.getRows().isEmpty()) {
					lastline = examples.getRows().get(examples.getRows().size() - 1).getLine(); 
				}
				return lastline;
			}
		});
		
		try {
			p.parse(document.get(), "", 0);
		} catch (LexingError le) {
			// TODO: log
		} catch (ParseError pe) {
			// TODO: log
		}
	}
}
