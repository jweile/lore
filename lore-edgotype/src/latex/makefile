build:
	pdflatex edgotyping && bibtex edgotyping && pdflatex edgotyping && pdflatex edgotyping

move:
	mkdir -p ../../target/latex/aux && mv *.pdf ../../target/latex && mv *.aux *.bbl *.blg *.log ../../target/latex/aux
