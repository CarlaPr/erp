SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

CREATE TABLE public.accounts_payable (
                                         id uuid NOT NULL,
                                         category character varying(255) NOT NULL,
                                         description character varying(255) NOT NULL,
                                         document_number character varying(255),
                                         due_date date NOT NULL,
                                         notes text,
                                         paid_amount numeric(12,2),
                                         payment_date date,
                                         payment_method character varying(255),
                                         is_recurring boolean,
                                         status character varying(255) NOT NULL,
                                         subcategory character varying(255),
                                         total_amount numeric(12,2) NOT NULL,
                                         supplier_id uuid,
                                         work_order_id uuid,
                                         reconciliation_status character varying(255)
);

CREATE TABLE public.accounts_receivable (
                                            id uuid NOT NULL,
                                            card_fee_percentage numeric(5,2),
                                            description character varying(255) NOT NULL,
                                            discount numeric(12,2),
                                            due_date date NOT NULL,
                                            installments integer,
                                            notes text,
                                            payment_date date,
                                            payment_method character varying(255),
                                            received_amount numeric(12,2),
                                            status character varying(255) NOT NULL,
                                            total_amount numeric(12,2) NOT NULL,
                                            client_id uuid,
                                            work_order_id uuid,
                                            fee_amount numeric(12,2),
                                            reconciliation_status character varying(255),
                                            gross_received_amount numeric(12,2)
);

CREATE TABLE public.app_users (
                                  id uuid NOT NULL,
                                  password character varying(255) NOT NULL,
                                  role character varying(255) NOT NULL,
                                  username character varying(255) NOT NULL
);

CREATE TABLE public.bank_accounts (
                                      id uuid NOT NULL,
                                      bank_name character varying(255),
                                      current_balance numeric(38,2),
                                      name character varying(255) NOT NULL,
                                      type character varying(255)
);

CREATE TABLE public.bank_transactions (
                                          id uuid NOT NULL,
                                          amount numeric(12,2) NOT NULL,
                                          description character varying(255) NOT NULL,
                                          status character varying(255) NOT NULL,
                                          transaction_date date NOT NULL,
                                          type character varying(255) NOT NULL,
                                          bank_account_id uuid,
                                          divergence_amount numeric(12,2),
                                          external_id character varying(255),
                                          reconciliation_note text,
                                          matched_payable_id uuid,
                                          matched_receivable_id uuid
);

CREATE TABLE public.clients (
                                id uuid NOT NULL,
                                address character varying(255),
                                city character varying(255),
                                created_at timestamp(6) without time zone,
                                document character varying(255),
                                email character varying(255),
                                is_active boolean,
                                name character varying(255) NOT NULL,
                                notes text,
                                phone character varying(255),
                                type character varying(20),
                                updated_at timestamp(6) without time zone
);

CREATE TABLE public.expense_allocations (
                                            id uuid NOT NULL,
                                            percentage numeric(5,2) NOT NULL,
                                            allocation_value numeric(12,2),
                                            accounts_payable_id uuid NOT NULL,
                                            work_order_id uuid NOT NULL
);

CREATE TABLE public.losses (
                               id uuid NOT NULL,
                               description character varying(255) NOT NULL,
                               financial_impact numeric(12,2) NOT NULL,
                               material character varying(255) NOT NULL,
                               occurrence_date date NOT NULL,
                               type character varying(255) NOT NULL,
                               work_order_id uuid
);

CREATE TABLE public.material_categories (
                                            id uuid NOT NULL,
                                            created_at timestamp(6) without time zone,
                                            name character varying(255) NOT NULL,
                                            type character varying(255)
);

CREATE TABLE public.profiles (
                                 id uuid NOT NULL,
                                 address character varying(255),
                                 city character varying(255),
                                 company_name character varying(255),
                                 document character varying(255),
                                 email character varying(255),
                                 logo_url character varying(255),
                                 owner_name character varying(255),
                                 phone character varying(255),
                                 signature_url character varying(255),
                                 tax_rate numeric(38,2)
);

CREATE TABLE public.quote_items (
                                    id uuid NOT NULL,
                                    category character varying(255),
                                    description character varying(255),
                                    height numeric(10,2),
                                    product character varying(255),
                                    quantity numeric(10,2),
                                    unit_price numeric(12,2),
                                    width numeric(10,2),
                                    quote_id uuid
);

CREATE TABLE public.quotes (
                               id uuid NOT NULL,
                               date_created timestamp(6) without time zone,
                               installments integer,
                               number character varying(20) NOT NULL,
                               observations text,
                               payment_method character varying(50),
                               status character varying(50) NOT NULL,
                               total_value numeric(12,2),
                               warranty text,
                               client_id uuid
);

CREATE TABLE public.service_categories (
                                           id uuid NOT NULL,
                                           description character varying(255),
                                           name character varying(255) NOT NULL
);

CREATE TABLE public.suppliers (
                                  id uuid NOT NULL,
                                  address character varying(255),
                                  category character varying(255),
                                  city character varying(255),
                                  created_at timestamp(6) without time zone,
                                  document character varying(255),
                                  email character varying(255),
                                  is_active boolean,
                                  name character varying(255) NOT NULL,
                                  notes text,
                                  phone character varying(255)
);

CREATE TABLE public.work_order_items (
                                         id uuid NOT NULL,
                                         description character varying(255) NOT NULL,
                                         quantity numeric(10,2) NOT NULL,
                                         unit_cost numeric(12,2) NOT NULL,
                                         unit_price numeric(12,2) NOT NULL,
                                         work_order_id uuid NOT NULL
);

CREATE TABLE public.work_orders (
                                    id uuid NOT NULL,
                                    area numeric(38,2),
                                    created_at timestamp(6) without time zone,
                                    description text,
                                    height numeric(38,2),
                                    install_date date,
                                    notes character varying(255),
                                    number character varying(255),
                                    status character varying(255),
                                    title character varying(255),
                                    total_value numeric(38,2),
                                    width numeric(38,2),
                                    category_id uuid,
                                    client_id uuid,
                                    quote_id uuid
);

ALTER TABLE ONLY public.accounts_payable
    ADD CONSTRAINT accounts_payable_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.accounts_receivable
    ADD CONSTRAINT accounts_receivable_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.app_users
    ADD CONSTRAINT app_users_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.bank_accounts
    ADD CONSTRAINT bank_accounts_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.bank_transactions
    ADD CONSTRAINT bank_transactions_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.clients
    ADD CONSTRAINT clients_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.expense_allocations
    ADD CONSTRAINT expense_allocations_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.losses
    ADD CONSTRAINT losses_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.material_categories
    ADD CONSTRAINT material_categories_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.profiles
    ADD CONSTRAINT profiles_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.quote_items
    ADD CONSTRAINT quote_items_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.quotes
    ADD CONSTRAINT quotes_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.service_categories
    ADD CONSTRAINT service_categories_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.suppliers
    ADD CONSTRAINT suppliers_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.work_orders
    ADD CONSTRAINT uk4jd1e6xavvijygu0qejlpp9i3 UNIQUE (quote_id);

ALTER TABLE ONLY public.quotes
    ADD CONSTRAINT uk6xnt1yuqlpv2irqg6dj9mvbnp UNIQUE (number);

ALTER TABLE ONLY public.app_users
    ADD CONSTRAINT ukspsnwr241e9k9c8p5xl4k45ih UNIQUE (username);

ALTER TABLE ONLY public.work_order_items
    ADD CONSTRAINT work_order_items_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.work_orders
    ADD CONSTRAINT work_orders_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.work_orders
    ADD CONSTRAINT fk3bxpg6owc5c90xmcg01kgblwi FOREIGN KEY (quote_id) REFERENCES public.quotes(id);

ALTER TABLE ONLY public.accounts_payable
    ADD CONSTRAINT fk6364ffhgqwhxd4nrb9mfg5hs8 FOREIGN KEY (work_order_id) REFERENCES public.work_orders(id);

ALTER TABLE ONLY public.accounts_receivable
    ADD CONSTRAINT fk693yeo5haupsrduvclf2jxxt FOREIGN KEY (work_order_id) REFERENCES public.work_orders(id);

ALTER TABLE ONLY public.work_orders
    ADD CONSTRAINT fk9g4kie5aon1shi8w8gnpuwxg6 FOREIGN KEY (client_id) REFERENCES public.clients(id);

ALTER TABLE ONLY public.accounts_receivable
    ADD CONSTRAINT fkaw3xc3fxaqt3qacoga8d277o4 FOREIGN KEY (client_id) REFERENCES public.clients(id);

ALTER TABLE ONLY public.work_order_items
    ADD CONSTRAINT fkdh09pf5h5ia4e0wicoq0c1hlv FOREIGN KEY (work_order_id) REFERENCES public.work_orders(id);

ALTER TABLE ONLY public.work_orders
    ADD CONSTRAINT fkekejkg4hd9er9hja5350jf9jb FOREIGN KEY (category_id) REFERENCES public.service_categories(id);

ALTER TABLE ONLY public.quotes
    ADD CONSTRAINT fkgbnmc624hyny4k4q8etbxxmu1 FOREIGN KEY (client_id) REFERENCES public.clients(id);

ALTER TABLE ONLY public.losses
    ADD CONSTRAINT fkjixb7qd7gdl1ub6iek4kqqujn FOREIGN KEY (work_order_id) REFERENCES public.work_orders(id);

ALTER TABLE ONLY public.bank_transactions
    ADD CONSTRAINT fklcu39w7fixvl9kv21tfx9tbhm FOREIGN KEY (bank_account_id) REFERENCES public.bank_accounts(id);

ALTER TABLE ONLY public.bank_transactions
    ADD CONSTRAINT fklo1jt30meqwehtkwhwyk82kma FOREIGN KEY (matched_receivable_id) REFERENCES public.accounts_receivable(id);

ALTER TABLE ONLY public.accounts_payable
    ADD CONSTRAINT fkmvlr9wfyxi38xpyhsjv2os6aq FOREIGN KEY (supplier_id) REFERENCES public.suppliers(id);

ALTER TABLE ONLY public.expense_allocations
    ADD CONSTRAINT fkrf6g9vb3lgr2w5yea63vkayg0 FOREIGN KEY (accounts_payable_id) REFERENCES public.accounts_payable(id);

ALTER TABLE ONLY public.expense_allocations
    ADD CONSTRAINT fkrnccbb4sjbjkpec0ir3d0ewxc FOREIGN KEY (work_order_id) REFERENCES public.work_orders(id);

ALTER TABLE ONLY public.quote_items
    ADD CONSTRAINT fkrvsmoef7yontnlu1lwxrb0g3g FOREIGN KEY (quote_id) REFERENCES public.quotes(id);

ALTER TABLE ONLY public.bank_transactions
    ADD CONSTRAINT fktd9d1vklbia8qw8g2j06oslcu FOREIGN KEY (matched_payable_id) REFERENCES public.accounts_payable(id);