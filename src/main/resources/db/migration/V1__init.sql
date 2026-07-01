--
-- PostgreSQL database dump
--

-- Dumped from database version 18.4
-- Dumped by pg_dump version 18.4

-- Started on 2026-06-30 11:13:30

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

--
-- TOC entry 219 (class 1259 OID 16389)
-- Name: accounts_payable; Type: TABLE; Schema: public; Owner: postgres
--

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


ALTER TABLE public.accounts_payable;

--
-- TOC entry 220 (class 1259 OID 16402)
-- Name: accounts_receivable; Type: TABLE; Schema: public; Owner: postgres
--

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


ALTER TABLE public.accounts_receivable ;

--
-- TOC entry 221 (class 1259 OID 16414)
-- Name: app_users; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.app_users (
                                  id uuid NOT NULL,
                                  password character varying(255) NOT NULL,
                                  role character varying(255) NOT NULL,
                                  username character varying(255) NOT NULL
);


ALTER TABLE public.app_users;

--
-- TOC entry 222 (class 1259 OID 16425)
-- Name: bank_accounts; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.bank_accounts (
                                      id uuid NOT NULL,
                                      bank_name character varying(255),
                                      current_balance numeric(38,2),
                                      name character varying(255) NOT NULL,
                                      type character varying(255)
);


ALTER TABLE public.bank_accounts ;

--
-- TOC entry 223 (class 1259 OID 16434)
-- Name: bank_transactions; Type: TABLE; Schema: public; Owner: postgres
--

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


ALTER TABLE public.bank_transactions ;

--
-- TOC entry 224 (class 1259 OID 16447)
-- Name: clients; Type: TABLE; Schema: public; Owner: postgres
--

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


ALTER TABLE public.clients;

--
-- TOC entry 225 (class 1259 OID 16456)
-- Name: expense_allocations; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.expense_allocations (
                                            id uuid NOT NULL,
                                            percentage numeric(5,2) NOT NULL,
                                            allocation_value numeric(12,2),
                                            accounts_payable_id uuid NOT NULL,
                                            work_order_id uuid NOT NULL
);


ALTER TABLE public.expense_allocations;

--
-- TOC entry 226 (class 1259 OID 16465)
-- Name: losses; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.losses (
                               id uuid NOT NULL,
                               description character varying(255) NOT NULL,
                               financial_impact numeric(12,2) NOT NULL,
                               material character varying(255) NOT NULL,
                               occurrence_date date NOT NULL,
                               type character varying(255) NOT NULL,
                               work_order_id uuid
);


ALTER TABLE public.losses;

--
-- TOC entry 227 (class 1259 OID 16478)
-- Name: material_categories; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.material_categories (
                                            id uuid NOT NULL,
                                            created_at timestamp(6) without time zone,
                                            name character varying(255) NOT NULL,
                                            type character varying(255)
);


ALTER TABLE public.material_categories;

--
-- TOC entry 228 (class 1259 OID 16487)
-- Name: profiles; Type: TABLE; Schema: public; Owner: postgres
--

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


ALTER TABLE public.profiles;

--
-- TOC entry 229 (class 1259 OID 16495)
-- Name: quote_items; Type: TABLE; Schema: public; Owner: postgres
--

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


ALTER TABLE public.quote_items;

--
-- TOC entry 230 (class 1259 OID 16503)
-- Name: quotes; Type: TABLE; Schema: public; Owner: postgres
--

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


ALTER TABLE public.quotes ;

--
-- TOC entry 231 (class 1259 OID 16513)
-- Name: service_categories; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.service_categories (
                                           id uuid NOT NULL,
                                           description character varying(255),
                                           name character varying(255) NOT NULL
);


ALTER TABLE public.service_categories;

--
-- TOC entry 232 (class 1259 OID 16522)
-- Name: suppliers; Type: TABLE; Schema: public; Owner: postgres
--

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


ALTER TABLE public.suppliers ;

--
-- TOC entry 233 (class 1259 OID 16531)
-- Name: work_order_items; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.work_order_items (
                                         id uuid NOT NULL,
                                         description character varying(255) NOT NULL,
                                         quantity numeric(10,2) NOT NULL,
                                         unit_cost numeric(12,2) NOT NULL,
                                         unit_price numeric(12,2) NOT NULL,
                                         work_order_id uuid NOT NULL
);


ALTER TABLE public.work_order_items ;

--
-- TOC entry 234 (class 1259 OID 16542)
-- Name: work_orders; Type: TABLE; Schema: public; Owner: postgres
--

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


ALTER TABLE public.work_orders ;

--
-- TOC entry 4815 (class 2606 OID 16401)
-- Name: accounts_payable accounts_payable_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.accounts_payable
    ADD CONSTRAINT accounts_payable_pkey PRIMARY KEY (id);


--
-- TOC entry 4817 (class 2606 OID 16413)
-- Name: accounts_receivable accounts_receivable_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.accounts_receivable
    ADD CONSTRAINT accounts_receivable_pkey PRIMARY KEY (id);


--
-- TOC entry 4819 (class 2606 OID 16424)
-- Name: app_users app_users_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.app_users
    ADD CONSTRAINT app_users_pkey PRIMARY KEY (id);


--
-- TOC entry 4823 (class 2606 OID 16433)
-- Name: bank_accounts bank_accounts_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.bank_accounts
    ADD CONSTRAINT bank_accounts_pkey PRIMARY KEY (id);


--
-- TOC entry 4825 (class 2606 OID 16446)
-- Name: bank_transactions bank_transactions_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.bank_transactions
    ADD CONSTRAINT bank_transactions_pkey PRIMARY KEY (id);


--
-- TOC entry 4827 (class 2606 OID 16455)
-- Name: clients clients_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.clients
    ADD CONSTRAINT clients_pkey PRIMARY KEY (id);


--
-- TOC entry 4829 (class 2606 OID 16464)
-- Name: expense_allocations expense_allocations_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.expense_allocations
    ADD CONSTRAINT expense_allocations_pkey PRIMARY KEY (id);


--
-- TOC entry 4831 (class 2606 OID 16477)
-- Name: losses losses_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.losses
    ADD CONSTRAINT losses_pkey PRIMARY KEY (id);


--
-- TOC entry 4833 (class 2606 OID 16486)
-- Name: material_categories material_categories_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.material_categories
    ADD CONSTRAINT material_categories_pkey PRIMARY KEY (id);


--
-- TOC entry 4835 (class 2606 OID 16494)
-- Name: profiles profiles_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.profiles
    ADD CONSTRAINT profiles_pkey PRIMARY KEY (id);


--
-- TOC entry 4837 (class 2606 OID 16502)
-- Name: quote_items quote_items_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.quote_items
    ADD CONSTRAINT quote_items_pkey PRIMARY KEY (id);


--
-- TOC entry 4839 (class 2606 OID 16512)
-- Name: quotes quotes_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.quotes
    ADD CONSTRAINT quotes_pkey PRIMARY KEY (id);


--
-- TOC entry 4843 (class 2606 OID 16521)
-- Name: service_categories service_categories_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.service_categories
    ADD CONSTRAINT service_categories_pkey PRIMARY KEY (id);


--
-- TOC entry 4845 (class 2606 OID 16530)
-- Name: suppliers suppliers_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.suppliers
    ADD CONSTRAINT suppliers_pkey PRIMARY KEY (id);


--
-- TOC entry 4849 (class 2606 OID 16555)
-- Name: work_orders uk4jd1e6xavvijygu0qejlpp9i3; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.work_orders
    ADD CONSTRAINT uk4jd1e6xavvijygu0qejlpp9i3 UNIQUE (quote_id);


--
-- TOC entry 4841 (class 2606 OID 16553)
-- Name: quotes uk6xnt1yuqlpv2irqg6dj9mvbnp; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.quotes
    ADD CONSTRAINT uk6xnt1yuqlpv2irqg6dj9mvbnp UNIQUE (number);


--
-- TOC entry 4821 (class 2606 OID 16551)
-- Name: app_users ukspsnwr241e9k9c8p5xl4k45ih; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.app_users
    ADD CONSTRAINT ukspsnwr241e9k9c8p5xl4k45ih UNIQUE (username);


--
-- TOC entry 4847 (class 2606 OID 16541)
-- Name: work_order_items work_order_items_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.work_order_items
    ADD CONSTRAINT work_order_items_pkey PRIMARY KEY (id);


--
-- TOC entry 4851 (class 2606 OID 16549)
-- Name: work_orders work_orders_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.work_orders
    ADD CONSTRAINT work_orders_pkey PRIMARY KEY (id);


--
-- TOC entry 4865 (class 2606 OID 16621)
-- Name: work_orders fk3bxpg6owc5c90xmcg01kgblwi; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.work_orders
    ADD CONSTRAINT fk3bxpg6owc5c90xmcg01kgblwi FOREIGN KEY (quote_id) REFERENCES public.quotes(id);


--
-- TOC entry 4852 (class 2606 OID 16561)
-- Name: accounts_payable fk6364ffhgqwhxd4nrb9mfg5hs8; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.accounts_payable
    ADD CONSTRAINT fk6364ffhgqwhxd4nrb9mfg5hs8 FOREIGN KEY (work_order_id) REFERENCES public.work_orders(id);


--
-- TOC entry 4854 (class 2606 OID 16571)
-- Name: accounts_receivable fk693yeo5haupsrduvclf2jxxt; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.accounts_receivable
    ADD CONSTRAINT fk693yeo5haupsrduvclf2jxxt FOREIGN KEY (work_order_id) REFERENCES public.work_orders(id);


--
-- TOC entry 4866 (class 2606 OID 16616)
-- Name: work_orders fk9g4kie5aon1shi8w8gnpuwxg6; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.work_orders
    ADD CONSTRAINT fk9g4kie5aon1shi8w8gnpuwxg6 FOREIGN KEY (client_id) REFERENCES public.clients(id);


--
-- TOC entry 4855 (class 2606 OID 16566)
-- Name: accounts_receivable fkaw3xc3fxaqt3qacoga8d277o4; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.accounts_receivable
    ADD CONSTRAINT fkaw3xc3fxaqt3qacoga8d277o4 FOREIGN KEY (client_id) REFERENCES public.clients(id);


--
-- TOC entry 4864 (class 2606 OID 16606)
-- Name: work_order_items fkdh09pf5h5ia4e0wicoq0c1hlv; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.work_order_items
    ADD CONSTRAINT fkdh09pf5h5ia4e0wicoq0c1hlv FOREIGN KEY (work_order_id) REFERENCES public.work_orders(id);


--
-- TOC entry 4867 (class 2606 OID 16611)
-- Name: work_orders fkekejkg4hd9er9hja5350jf9jb; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.work_orders
    ADD CONSTRAINT fkekejkg4hd9er9hja5350jf9jb FOREIGN KEY (category_id) REFERENCES public.service_categories(id);


--
-- TOC entry 4863 (class 2606 OID 16601)
-- Name: quotes fkgbnmc624hyny4k4q8etbxxmu1; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.quotes
    ADD CONSTRAINT fkgbnmc624hyny4k4q8etbxxmu1 FOREIGN KEY (client_id) REFERENCES public.clients(id);


--
-- TOC entry 4861 (class 2606 OID 16591)
-- Name: losses fkjixb7qd7gdl1ub6iek4kqqujn; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.losses
    ADD CONSTRAINT fkjixb7qd7gdl1ub6iek4kqqujn FOREIGN KEY (work_order_id) REFERENCES public.work_orders(id);


--
-- TOC entry 4856 (class 2606 OID 16576)
-- Name: bank_transactions fklcu39w7fixvl9kv21tfx9tbhm; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.bank_transactions
    ADD CONSTRAINT fklcu39w7fixvl9kv21tfx9tbhm FOREIGN KEY (bank_account_id) REFERENCES public.bank_accounts(id);


--
-- TOC entry 4857 (class 2606 OID 16633)
-- Name: bank_transactions fklo1jt30meqwehtkwhwyk82kma; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.bank_transactions
    ADD CONSTRAINT fklo1jt30meqwehtkwhwyk82kma FOREIGN KEY (matched_receivable_id) REFERENCES public.accounts_receivable(id);


--
-- TOC entry 4853 (class 2606 OID 16556)
-- Name: accounts_payable fkmvlr9wfyxi38xpyhsjv2os6aq; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.accounts_payable
    ADD CONSTRAINT fkmvlr9wfyxi38xpyhsjv2os6aq FOREIGN KEY (supplier_id) REFERENCES public.suppliers(id);


--
-- TOC entry 4859 (class 2606 OID 16581)
-- Name: expense_allocations fkrf6g9vb3lgr2w5yea63vkayg0; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.expense_allocations
    ADD CONSTRAINT fkrf6g9vb3lgr2w5yea63vkayg0 FOREIGN KEY (accounts_payable_id) REFERENCES public.accounts_payable(id);


--
-- TOC entry 4860 (class 2606 OID 16586)
-- Name: expense_allocations fkrnccbb4sjbjkpec0ir3d0ewxc; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.expense_allocations
    ADD CONSTRAINT fkrnccbb4sjbjkpec0ir3d0ewxc FOREIGN KEY (work_order_id) REFERENCES public.work_orders(id);


--
-- TOC entry 4862 (class 2606 OID 16596)
-- Name: quote_items fkrvsmoef7yontnlu1lwxrb0g3g; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.quote_items
    ADD CONSTRAINT fkrvsmoef7yontnlu1lwxrb0g3g FOREIGN KEY (quote_id) REFERENCES public.quotes(id);


--
-- TOC entry 4858 (class 2606 OID 16628)
-- Name: bank_transactions fktd9d1vklbia8qw8g2j06oslcu; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.bank_transactions
    ADD CONSTRAINT fktd9d1vklbia8qw8g2j06oslcu FOREIGN KEY (matched_payable_id) REFERENCES public.accounts_payable(id);


-- Completed on 2026-06-30 11:13:30

--
-- PostgreSQL database dump complete
--

