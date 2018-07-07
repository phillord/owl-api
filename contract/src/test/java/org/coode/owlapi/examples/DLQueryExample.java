/*
 * This file is part of the OWL API.
 *
 * The contents of this file are subject to the LGPL License, Version 3.0.
 *
 * Copyright (C) 2011, The University of Manchester
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0
 * in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 *
 * Copyright 2011, The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.coode.owlapi.examples;

//import org.semanticweb.HermiT.Reasoner;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.expression.OWLEntityChecker;
import org.semanticweb.owlapi.expression.ShortFormEntityChecker;
import org.semanticweb.owlapi.manchestersyntax.parser.ManchesterOWLSyntaxParserImpl;
import org.semanticweb.owlapi.manchestersyntax.renderer.ParserException;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OntologyConfigurator;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.structural.StructuralReasonerFactory;
import org.semanticweb.owlapi.util.BidirectionalShortFormProvider;
import org.semanticweb.owlapi.util.BidirectionalShortFormProviderAdapter;
import org.semanticweb.owlapi.util.ShortFormProvider;
import org.semanticweb.owlapi.util.SimpleShortFormProvider;
import org.semanticweb.owlapi.util.mansyntax.ManchesterOWLSyntaxParser;

/** Author: Matthew Horridge<br>
 * The University of Manchester<br>
 * Bio-Health Informatics Group<br>
 * Date: 13-May-2010 <br>
 * An example that shows how to do a Protege like DLQuery. The example contains
 * several helper classes:<br>
 * DLQueryEngine - This takes a string representing a class expression built
 * from the terms in the signature of some ontology. DLQueryPrinter - This takes
 * a string class expression and prints out the sub/super/equivalent classes and
 * the instances of the specified class expression. DLQueryParser - this parses
 * the specified class expression string */
public class DLQueryExample {
    private static final IRI ONTOLOGY_IRI = IRI
            .create("http://www.co-ode.org/ontologies/pizza/pizza.owl");

    @SuppressWarnings("javadoc")
    public static void main(String[] args) {
        try {
            // Load an example ontology. In this case, we'll just load the pizza
            // ontology.
            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
            OWLOntology ontology = manager.loadOntologyFromOntologyDocument(ONTOLOGY_IRI);
            System.out.println("Loaded ontology: " + ontology.getOntologyID());
            // We need a reasoner to do our query answering
            OWLReasoner reasoner = createReasoner(ontology);
            // Entities are named using IRIs. These are usually too long for use
            // in user interfaces. To solve this
            // problem, and so a query can be written using short class,
            // property, individual names we use a short form
            // provider. In this case, we'll just use a simple short form
            // provider that generates short froms from IRI
            // fragments.
            ShortFormProvider shortFormProvider = new SimpleShortFormProvider();
            // Create the DLQueryPrinter helper class. This will manage the
            // parsing of input and printing of results
            DLQueryPrinter dlQueryPrinter = new DLQueryPrinter(new DLQueryEngine(
                    reasoner, shortFormProvider), shortFormProvider);
            // Enter the query loop. A user is expected to enter class
            // expression on the command line.
            doQueryLoop(dlQueryPrinter);
        } catch (OWLOntologyCreationException e) {
            System.out.println("Could not load ontology: " + e.getMessage());
        } catch (IOException ioEx) {
            System.out.println(ioEx.getMessage());
        }
    }

    private static void doQueryLoop(DLQueryPrinter dlQueryPrinter) throws IOException {
        while (true) {
            // Prompt the user to enter a class expression
            System.out
                    .println("Please type a class expression in Manchester Syntax and press Enter (or press x to exit):");
            System.out.println("");
            String classExpression = readInput();
            // Check for exit condition
            if (classExpression.equalsIgnoreCase("x")) {
                break;
            }
            dlQueryPrinter.askQuery(classExpression.trim());
            System.out.println();
            System.out.println();
        }
    }

    private static String readInput() throws IOException {
        InputStream is = System.in;
        InputStreamReader reader;
        reader = new InputStreamReader(is, "UTF-8");
        BufferedReader br = new BufferedReader(reader);
        return br.readLine();
    }

    private static OWLReasoner createReasoner(final OWLOntology rootOntology) {
        // We need to create an instance of OWLReasoner. An OWLReasoner provides
        // the basic query functionality that we need, for example the ability
        // obtain the subclasses of a class etc. To do this we use a reasoner
        // factory.
        // Create a reasoner factory.
        OWLReasonerFactory reasonerFactory = new StructuralReasonerFactory();
        return reasonerFactory.createReasoner(rootOntology);
    }
}

/** This example shows how to perform a "dlquery". The DLQuery view/tab in
 * Protege 4 works like this. */
class DLQueryEngine {
    private final OWLReasoner reasoner;
    private final DLQueryParser parser;

    /** Constructs a DLQueryEngine. This will answer "DL queries" using the
     * specified reasoner. A short form provider specifies how entities are
     * rendered.
     * 
     * @param reasoner
     *            The reasoner to be used for answering the queries.
     * @param shortFormProvider
     *            A short form provider. */
    public DLQueryEngine(OWLReasoner reasoner, ShortFormProvider shortFormProvider) {
        this.reasoner = reasoner;
        OWLOntology rootOntology = reasoner.getRootOntology();
        parser = new DLQueryParser(rootOntology, shortFormProvider);
    }

    /** Gets the superclasses of a class expression parsed from a string.
     * 
     * @param classExpressionString
     *            The string from which the class expression will be parsed.
     * @param direct
     *            Specifies whether direct superclasses should be returned or
     *            not.
     * @return The superclasses of the specified class expression
     * @throws ParserException
     *             If there was a problem parsing the class expression. */
    public Stream<OWLClass> getSuperClasses(String classExpressionString, boolean direct) throws ParserException {
        if (classExpressionString.trim().length() == 0) {
            return Stream.empty();
        }
        OWLClassExpression classExpression = parser
                .parseClassExpression(classExpressionString);
        NodeSet<OWLClass> superClasses = reasoner
                .getSuperClasses(classExpression, direct);
        return superClasses.entities();
    }

    /** Gets the equivalent classes of a class expression parsed from a string.
     * 
     * @param classExpressionString
     *            The string from which the class expression will be parsed.
     * @return The equivalent classes of the specified class expression
     * @throws ParserException
     *             If there was a problem parsing the class expression. */
    public Stream<OWLClass> getEquivalentClasses(String classExpressionString) throws ParserException{
        if (classExpressionString.trim().length() == 0) {
            return Stream.empty();
        }
        OWLClassExpression classExpression = parser
                .parseClassExpression(classExpressionString);
        Node<OWLClass> equivalentClasses = reasoner.getEquivalentClasses(classExpression);
        Stream<OWLClass> result;
        if (classExpression.isAnonymous()) {
            result = equivalentClasses.entities();
        } else {
            result = equivalentClasses.getEntitiesMinus(classExpression.asOWLClass()).stream();
        }
        return result;
    }

    /** Gets the subclasses of a class expression parsed from a string.
     * 
     * @param classExpressionString
     *            The string from which the class expression will be parsed.
     * @param direct
     *            Specifies whether direct subclasses should be returned or not.
     * @return The subclasses of the specified class expression
     * @throws ParserException
     *             If there was a problem parsing the class expression. */
    public Stream<OWLClass> getSubClasses(String classExpressionString, boolean direct) throws ParserException {
        if (classExpressionString.trim().length() == 0) {
            return Stream.empty();
        }
        OWLClassExpression classExpression = parser
                .parseClassExpression(classExpressionString);
        NodeSet<OWLClass> subClasses = reasoner.getSubClasses(classExpression, direct);
        return subClasses.entities();
    }

    /** Gets the instances of a class expression parsed from a string.
     * 
     * @param classExpressionString
     *            The string from which the class expression will be parsed.
     * @param direct
     *            Specifies whether direct instances should be returned or not.
     * @return The instances of the specified class expression
     * @throws ParserException
     *             If there was a problem parsing the class expression. */
    public Stream<OWLNamedIndividual> getInstances(String classExpressionString, boolean direct) throws ParserException {
        if (classExpressionString.trim().length() == 0) {
            return Stream.empty();
        }
        OWLClassExpression classExpression = parser
                .parseClassExpression(classExpressionString);
        NodeSet<OWLNamedIndividual> individuals = reasoner.getInstances(classExpression,
                direct);
        return individuals.entities();
    }
}

class DLQueryParser {
    private final OWLOntology rootOntology;
    private final BidirectionalShortFormProvider bidiShortFormProvider;

    /** Constructs a DLQueryParser using the specified ontology and short form
     * provider to map entity IRIs to short names.
     * 
     * @param rootOntology
     *            The root ontology. This essentially provides the domain
     *            vocabulary for the query.
     * @param shortFormProvider
     *            A short form provider to be used for mapping back and forth
     *            between entities and their short names (renderings). */
    public DLQueryParser(OWLOntology rootOntology, ShortFormProvider shortFormProvider) {
        this.rootOntology = rootOntology;
        OWLOntologyManager manager = rootOntology.getOWLOntologyManager();
        Stream<OWLOntology> importsClosure = rootOntology.importsClosure();
        // Create a bidirectional short form provider to do the actual mapping.
        // It will generate names using the input
        // short form provider.
        bidiShortFormProvider = new BidirectionalShortFormProviderAdapter(manager,
        		importsClosure.collect(Collectors.toSet()), shortFormProvider);
    }

    /** Parses a class expression string to obtain a class expression.
     * 
     * @param classExpressionString
     *            The class expression string
     * @return The corresponding class expression
     * @throws ParserException
     *             if the class expression string is malformed or contains
     *             unknown entity names. */
    public OWLClassExpression parseClassExpression(String classExpressionString) {
        OWLDataFactory dataFactory = rootOntology.getOWLOntologyManager()
                .getOWLDataFactory();
        // Set up the real parser
        ManchesterOWLSyntaxParser parser = new ManchesterOWLSyntaxParserImpl(
        		new OntologyConfigurator(), dataFactory);
        parser.setStringToParse(classExpressionString);
        parser.setDefaultOntology(rootOntology);
        // Specify an entity checker that wil be used to check a class
        // expression contains the correct names.
        OWLEntityChecker entityChecker = new ShortFormEntityChecker(bidiShortFormProvider);
        parser.setOWLEntityChecker(entityChecker);
        // Do the actual parsing
        return parser.parseClassExpression();
    }
}

class DLQueryPrinter {
    private final DLQueryEngine dlQueryEngine;
    private final ShortFormProvider shortFormProvider;

    /** @param engine
     *            the engine
     * @param shortFormProvider
     *            the short form provider */
    public DLQueryPrinter(DLQueryEngine engine, ShortFormProvider shortFormProvider) {
        this.shortFormProvider = shortFormProvider;
        dlQueryEngine = engine;
    }

    /** @param classExpression
     *            the class expression to use for interrogation */
    public void askQuery(String classExpression) {
        if (classExpression.length() == 0) {
            System.out.println("No class expression specified");
        } else {
            try {
                StringBuilder sb = new StringBuilder();
                sb.append("\n--------------------------------------------------------------------------------\n");
                sb.append("QUERY:   ");
                sb.append(classExpression);
                sb.append("\n");
                sb.append("--------------------------------------------------------------------------------\n\n");
                // Ask for the subclasses, superclasses etc. of the specified
                // class expression. Print out the results.
                Stream<OWLClass> superClasses = dlQueryEngine.getSuperClasses(classExpression, true);
                printEntities("SuperClasses", superClasses, sb);
                Stream<OWLClass> equivalentClasses = dlQueryEngine.getEquivalentClasses(classExpression);
                printEntities("EquivalentClasses", equivalentClasses, sb);
                Stream<OWLClass> subClasses = dlQueryEngine.getSubClasses(classExpression, true);
                printEntities("SubClasses", subClasses, sb);
                Stream<OWLNamedIndividual> individuals = dlQueryEngine.getInstances(classExpression, true);
                printEntities("Instances", individuals, sb);
                // System.out.println(sb.toString());
            } catch (ParserException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private void printEntities(String name, Stream<? extends OWLEntity> entities,
            StringBuilder sb) {
        sb.append(name);
        int length = 50 - name.length();
        for (int i = 0; i < length; i++) {
            sb.append(".");
        }
        sb.append("\n\n");
        Set<? extends OWLEntity> entitiesSet = entities.collect(Collectors.toSet());
        if (!entitiesSet.isEmpty()) {
            for (OWLEntity entity : entitiesSet) {
                sb.append("\t");
                sb.append(shortFormProvider.getShortForm(entity));
                sb.append("\n");
            }
        } else {
            sb.append("\t[NONE]\n");
        }
        sb.append("\n");
    }
}
