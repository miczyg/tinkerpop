package com.tinkerpop.gremlin.process.util;

import com.tinkerpop.gremlin.process.Step;
import com.tinkerpop.gremlin.process.Traversal;
import com.tinkerpop.gremlin.process.TraversalEngine;
import com.tinkerpop.gremlin.process.TraversalStrategies;
import com.tinkerpop.gremlin.process.Traverser;
import com.tinkerpop.gremlin.process.TraverserGenerator;
import com.tinkerpop.gremlin.process.graph.marker.TraverserSource;
import com.tinkerpop.gremlin.process.graph.strategy.GraphTraversalStrategyRegistry;
import com.tinkerpop.gremlin.structure.Graph;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class DefaultTraversal<S, E> implements Traversal<S, E>, Traversal.Admin<S, E> {

    private E lastEnd = null;
    private long lastEndCount = 0l;
    private boolean locked = false; // an optimization so getTraversalEngine().isEmpty() isn't required on each next()/hasNext()

    protected List<Step> steps = new ArrayList<>();
    protected DefaultTraversalSideEffects sideEffects = new DefaultTraversalSideEffects();
    protected Optional<TraversalEngine> traversalEngine = Optional.empty();

    static {
        final DefaultTraversalStrategies traversalStrategies = new DefaultTraversalStrategies();
        GraphTraversalStrategyRegistry.instance().getTraversalStrategies().forEach(traversalStrategies::addStrategy);
        TraversalStrategies.GlobalCache.registerStrategies(DefaultTraversal.class, traversalStrategies);
    }

    public DefaultTraversal() {
    }

    public DefaultTraversal(final Graph graph) {
        this();
        this.sideEffects().setGraph(graph);
    }

    @Override
    public Traversal.Admin<S, E> asAdmin() {
        return this;
    }

    @Override
    public void applyStrategies(final TraversalEngine engine) {
        if (!this.traversalEngine.isPresent()) {
            TraversalStrategies.GlobalCache.getStrategies(this.getClass()).apply(this, engine);
            this.traversalEngine = Optional.of(engine);
            this.locked = true;

            // generate the traverser iterators for all the traverser source steps in the traversal
            // do not do this if on a graph computer as the traversal vertex program is responsible for creating traversers
            if (engine.equals(TraversalEngine.COMPUTER))
                return;
            final TraverserGenerator traverserGenerator = TraversalStrategies.GlobalCache.getStrategies(this.getClass()).getTraverserGenerator(this);
            this.asAdmin().getSteps()
                    .stream()
                    .filter(step -> step instanceof TraverserSource)
                    .forEach(step -> ((TraverserSource) step).generateTraversers(traverserGenerator));
        }
    }

    @Override
    public Optional<TraversalEngine> getTraversalEngine() {
        return this.traversalEngine;
    }

    @Override
    public List<Step> getSteps() {
        return this.steps;
    }

    @Override
    public SideEffects sideEffects() {
        return this.sideEffects;
    }

    @Override
    public void addStarts(final Iterator<Traverser<S>> starts) {
        TraversalHelper.getStart(this).addStarts(starts);
    }

    @Override
    public void addStart(final Traverser<S> start) {
        TraversalHelper.getStart(this).addStart(start);
    }

    @Override
    public boolean hasNext() {
        if (!this.locked) this.applyStrategies(TraversalEngine.STANDARD);
        return this.lastEndCount > 0l || TraversalHelper.getEnd(this).hasNext();
    }

    @Override
    public E next() {
        if (!this.locked) this.applyStrategies(TraversalEngine.STANDARD);
        if (this.lastEndCount > 0l) {
            this.lastEndCount--;
            return this.lastEnd;
        } else {
            final Traverser<E> next = TraversalHelper.getEnd(this).next();
            if (next.bulk() == 1) {
                return next.get();
            } else {
                this.lastEndCount = next.bulk() - 1;
                this.lastEnd = next.get();
                return this.lastEnd;
            }
        }
    }

    @Override
    public String toString() {
        return TraversalHelper.makeTraversalString(this);
    }

    @Override
    public boolean equals(final Object object) {
        return object instanceof Iterator && TraversalHelper.areEqual(this, (Iterator) object);
    }

    @Override
    public DefaultTraversal<S, E> clone() throws CloneNotSupportedException {
        final DefaultTraversal<S, E> clone = (DefaultTraversal<S, E>) super.clone();
        clone.steps = new ArrayList<>();
        clone.sideEffects = this.sideEffects.clone();
        clone.lastEnd = null;
        clone.lastEndCount = 0l;
        for (int i = this.steps.size() - 1; i >= 0; i--) {
            final Step<?, ?> clonedStep = this.steps.get(i).clone();
            clonedStep.setTraversal(clone);
            TraversalHelper.insertStep(clonedStep, 0, clone);
        }
        return clone;
    }

    @Override
    public TraverserGenerator getTraverserGenerator() {
        return TraversalStrategies.GlobalCache.getStrategies(this.getClass()).getTraverserGenerator(this);
    }
}
