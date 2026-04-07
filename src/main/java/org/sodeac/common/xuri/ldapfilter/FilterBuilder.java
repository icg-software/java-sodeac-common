/*******************************************************************************
 * Copyright (c) 2020 Sebastian Palarus
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     Sebastian Palarus - initial API and implementation
 *******************************************************************************/
package org.sodeac.common.xuri.ldapfilter;

public class FilterBuilder
{
    private static final FilterBuilder INSTANCE = new FilterBuilder();
    
    private FilterBuilder()
    {
        super();
    }
    
    public static ILinkerBuilder andLinker()
    {
        return INSTANCE.new Linker(null, LogicalOperator.AND, false);
    }
    
    public static ILinkerBuilder nandLinker()
    {
        return INSTANCE.new Linker(null, LogicalOperator.AND, true);
    }
    
    public static ILinkerBuilder orLinker()
    {
        return INSTANCE.new Linker(null, LogicalOperator.OR, false);
    }
    
    public static ILinkerBuilder norLinker()
    {
        return INSTANCE.new Linker(null, LogicalOperator.OR, true);
    }
    
    public class Linker implements ISatisfiedLinkerBuilder
    {
        private Linker parent = null;
        private CriteriaLinker criteriaLinker = null;
        
        private Linker(final Linker parent, final LogicalOperator operator, final boolean invert)
        {
            super();
            this.parent = parent;
            this.criteriaLinker = new CriteriaLinker().setOperator(operator).setInvert(invert);
            if (this.parent != null)
            {
                this.parent.criteriaLinker.addItem(this.criteriaLinker);
            }
        }
        
        @Override
        public IOperand criteriaWithName(final String criteriaName)
        {
            return new Operand(criteriaName);
        }
        
        @Override
        public ILinkerBuilder nestedAndLinker()
        {
            return new Linker(this, LogicalOperator.AND, false);
        }
        
        @Override
        public ILinkerBuilder nestedNandLinker()
        {
            return new Linker(this, LogicalOperator.AND, true);
        }
        
        @Override
        public ILinkerBuilder nestedOrLinker()
        {
            return new Linker(this, LogicalOperator.OR, false);
        }
        
        @Override
        public ILinkerBuilder nestedNorLinker()
        {
            return new Linker(this, LogicalOperator.OR, true);
        }
        
        public class Operand implements IOperand
        {
            private String criteriaName = null;
            
            private Operand(final String criteriaName)
            {
                super();
                this.criteriaName = criteriaName;
            }
            
            @Override
            public ISatisfiedLinkerBuilder eq(final String criteriaValue)
            {
                Linker.this.criteriaLinker.addItem
                                              (
                                                  new Criteria().setName(this.criteriaName).setOperator(ComparativeOperator.EQUAL).setRawValue(escape(criteriaValue)).setInvert(false)
                                              );
                this.criteriaName = null;
                return Linker.this;
            }
            
            @Override
            public ISatisfiedLinkerBuilder notEq(final String criteriaValue)
            {
                Linker.this.criteriaLinker.addItem
                                              (
                                                  new Criteria().setName(this.criteriaName).setOperator(ComparativeOperator.EQUAL).setRawValue(escape(criteriaValue)).setInvert(true)
                                              );
                this.criteriaName = null;
                return Linker.this;
            }
            
            @Override
            public ISatisfiedLinkerBuilder approx(final String criteriaValue)
            {
                Linker.this.criteriaLinker.addItem
                                              (
                                                  new Criteria().setName(this.criteriaName).setOperator(ComparativeOperator.APPROX).setRawValue(escape(criteriaValue)).setInvert(false)
                                              );
                this.criteriaName = null;
                return Linker.this;
            }
            
            @Override
            public ISatisfiedLinkerBuilder notApprox(final String criteriaValue)
            {
                Linker.this.criteriaLinker.addItem
                                              (
                                                  new Criteria().setName(this.criteriaName).setOperator(ComparativeOperator.APPROX).setRawValue(escape(criteriaValue)).setInvert(true)
                                              );
                this.criteriaName = null;
                return Linker.this;
            }
            
            @Override
            public ISatisfiedLinkerBuilder gte(final String criteriaValue)
            {
                Linker.this.criteriaLinker.addItem
                                              (
                                                  new Criteria().setName(this.criteriaName).setOperator(ComparativeOperator.GTE).setRawValue(escape(criteriaValue)).setInvert(false)
                                              );
                this.criteriaName = null;
                return Linker.this;
            }
            
            @Override
            public ISatisfiedLinkerBuilder notGte(final String criteriaValue)
            {
                Linker.this.criteriaLinker.addItem
                                              (
                                                  new Criteria().setName(this.criteriaName).setOperator(ComparativeOperator.GTE).setRawValue(escape(criteriaValue)).setInvert(true)
                                              );
                this.criteriaName = null;
                return Linker.this;
            }
            
            @Override
            public ISatisfiedLinkerBuilder lte(final String criteriaValue)
            {
                Linker.this.criteriaLinker.addItem
                                              (
                                                  new Criteria().setName(this.criteriaName).setOperator(ComparativeOperator.LTE).setRawValue(escape(criteriaValue)).setInvert(false)
                                              );
                this.criteriaName = null;
                return Linker.this;
            }
            
            @Override
            public ISatisfiedLinkerBuilder notLte(final String criteriaValue)
            {
                Linker.this.criteriaLinker.addItem
                                              (
                                                  new Criteria().setName(this.criteriaName).setOperator(ComparativeOperator.LTE).setRawValue(escape(criteriaValue)).setInvert(true)
                                              );
                this.criteriaName = null;
                return Linker.this;
            }
            
            private String escape(final String criteriaValue)
            {
                if (criteriaValue == null)
                {
                    return null;
                }
                
                return criteriaValue.replaceAll("\\\\", "\\\\5c").replaceAll("\\(", "\\\\28").replaceAll("\\)", "\\\\29");
            }
        }
        
        @Override
        public ISatisfiedLinkerBuilder closeLinker()
        {
            if (this.parent != null)
            {
                Linker linker = this.parent;
                this.parent = null;
                this.criteriaLinker = null;
                return linker;
            }
            return this;
        }
        
        @Override
        public IFilterItem build()
        {
            Linker root = getRoot(this);
            CriteriaLinker criteriaLinker = root.criteriaLinker;
            root.criteriaLinker = null;
            if
            (
                (criteriaLinker.getLinkedItemList().size() == 1) &&
                (criteriaLinker.getLinkedItemList().get(0) instanceof final Criteria criteria)
            )
            {
                if (criteriaLinker.isInvert())
                {
                    criteria.setInvert(!criteria.isInvert());
                }
                return criteria;
            }
            return criteriaLinker;
        }
        
        private Linker getRoot(Linker linker)
        {
            if (linker == null)
            {
                return linker;
            }
            
            while (true)
            {
                if (linker.parent == null)
                {
                    return linker;
                }
                
                linker = (Linker) linker.closeLinker();
            }
        }
    }
    
    public interface ISatisfiedLinkerBuilder extends ILinkerBuilder
    {
        ISatisfiedLinkerBuilder closeLinker();
        
        IFilterItem build();
    }
    
    public interface ILinkerBuilder
    {
        IOperand criteriaWithName(String criteriaName);
        
        ILinkerBuilder nestedAndLinker();
        
        ILinkerBuilder nestedNandLinker();
        
        ILinkerBuilder nestedOrLinker();
        
        ILinkerBuilder nestedNorLinker();
        
        interface IOperand
        {
            ISatisfiedLinkerBuilder eq(String criteriaValue);
            
            ISatisfiedLinkerBuilder notEq(String criteriaValue);
            
            ISatisfiedLinkerBuilder approx(String criteriaValue);
            
            ISatisfiedLinkerBuilder notApprox(String criteriaValue);
            
            ISatisfiedLinkerBuilder gte(String criteriaValue);
            
            ISatisfiedLinkerBuilder notGte(String criteriaValue);
            
            ISatisfiedLinkerBuilder lte(String criteriaValue);
            
            ISatisfiedLinkerBuilder notLte(String criteriaValue);
        }
    }
}
