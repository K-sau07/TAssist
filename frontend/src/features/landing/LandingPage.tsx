import { useRef } from 'react'
import { Link } from 'react-router-dom'
import { motion } from 'framer-motion'
import { Upload, MessagesSquare, BookOpen, ShieldCheck, Users, FileLock2, Moon, Zap } from 'lucide-react'
import { MarginNoteDemo } from './parts/MarginNoteDemo'
import { Button } from '@/design/components/Button'
import { useAuthStore } from '@/lib/auth/store'

const reveal = {
  initial: { opacity: 0, y: 16 },
  whileInView: { opacity: 1, y: 0 },
  viewport: { once: true, margin: '-80px' },
  transition: { type: 'spring', stiffness: 140, damping: 22 },
} as const

export default function LandingPage() {
  const loggedIn = useAuthStore((s) => Boolean(s.token))
  const howRef = useRef<HTMLElement>(null)

  return (
    <div className="min-h-screen bg-bg text-text">
      {/* nav */}
      <header className="sticky top-0 z-30 border-b border-border/60 bg-bg/80 backdrop-blur">
        <nav className="mx-auto flex max-w-6xl items-center justify-between px-6 py-4">
          <Link to="/" className="font-display text-xl text-primary">TAssist</Link>
          <div className="flex items-center gap-2">
            {loggedIn
              ? <Link to="/app"><Button>Open app</Button></Link>
              : <>
                  <Link to="/login"><Button variant="ghost">Log in</Button></Link>
                  <Link to="/signup"><Button>Sign up</Button></Link>
                </>}
          </div>
        </nav>
      </header>

      {/* hero */}
      <section className="mx-auto grid max-w-6xl items-center gap-10 px-6 py-16 md:grid-cols-2 md:py-24">
        <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }}
          transition={{ type: 'spring', stiffness: 140, damping: 22 }}>
          <p className="mb-3 text-2xs font-semibold uppercase tracking-[0.2em] text-text-faint">
            The teaching assistant for your documents
          </p>
          <h1 className="font-display text-4xl leading-[1.1]">
            A calm place to study,<br />where every answer<br /><span className="text-primary">shows its work.</span>
          </h1>
          <p className="mt-5 max-w-md text-lg text-text-muted">
            Ask questions grounded only in your course documents — with citations you can trace to the exact
            lecture and slide. Never a hallucination.
          </p>
          <div className="mt-8 flex flex-wrap gap-3">
            <Link to="/signup"><Button size="lg">Start your study space</Button></Link>
            <Button size="lg" variant="secondary"
              onClick={() => howRef.current?.scrollIntoView({ behavior: 'smooth' })}>
              See how it works
            </Button>
          </div>
        </motion.div>
        <MarginNoteDemo />
      </section>

      {/* problem → solution */}
      <section className="border-y border-border bg-bg-sunken py-16">
        <div className="mx-auto max-w-3xl px-6 text-center">
          <motion.h2 {...reveal} className="font-display text-3xl leading-snug">
            Students ask the same questions at 2am. TAs answer them by hand, again and again.
          </motion.h2>
          <motion.p {...reveal} className="mx-auto mt-4 max-w-xl text-text-muted">
            TAssist turns a course's documents into a study space that answers — instantly, in plain language,
            and always with the source attached — so no one waits, and no one guesses.
          </motion.p>
        </div>
      </section>

      {/* how it works — a real sequence, so it's numbered */}
      <section ref={howRef} className="mx-auto max-w-6xl px-6 py-20">
        <motion.h2 {...reveal} className="text-center font-display text-3xl">How it works</motion.h2>
        <div className="mt-12 grid gap-6 md:grid-cols-3">
          {[
            { n: 1, icon: Upload, title: 'Add your documents', body: 'A TA uploads the syllabus, lecture slides, and notes into a channel.' },
            { n: 2, icon: MessagesSquare, title: 'Students ask', body: 'Anyone in the channel asks a question in plain English — or DMs the group.' },
            { n: 3, icon: BookOpen, title: 'Grounded answers', body: 'The AI answers only from those documents, and cites the exact source.' },
          ].map((s, i) => (
            <motion.div key={s.n} {...reveal} transition={{ ...reveal.transition, delay: i * 0.08 }}
              className="relative rounded-lg border border-border bg-bg-elev p-6 shadow-1 transition-transform hover:-translate-y-1 hover:shadow-2">
              <span className="absolute right-5 top-4 font-display text-3xl text-primary/15">{s.n}</span>
              <div className="grid h-12 w-12 place-items-center rounded-md bg-primary-wash text-primary">
                <s.icon size={22} strokeWidth={1.8} />
              </div>
              <h3 className="mt-4 font-display text-lg">{s.title}</h3>
              <p className="mt-1 text-sm text-text-muted">{s.body}</p>
            </motion.div>
          ))}
        </div>
      </section>

      {/* grounded, not guessed — the trust story */}
      <section className="mx-auto max-w-3xl px-6 pb-4">
        <motion.div {...reveal} className="rounded-xl border-l-[3px] border-l-[var(--primary-rule)] bg-primary-wash p-10 shadow-1">
          <ShieldCheck size={34} strokeWidth={1.5} className="text-primary" />
          <p className="mt-4 font-display text-2xl leading-snug">
            Grounded, not guessed. If the answer isn't in your documents, TAssist says so — it never makes one up.
          </p>
          <p className="mt-4 text-sm text-text-muted">
            Every answer carries numbered footnotes. Click one to open the exact passage it came from. That's the
            whole point: an answer you can verify in two seconds is an answer you can trust with your grade.
          </p>
        </motion.div>
      </section>

      {/* bento feature grid */}
      <section className="mx-auto max-w-6xl px-6 py-20">
        <motion.h2 {...reveal} className="text-center font-display text-3xl">Built for real coursework</motion.h2>
        <div className="mt-10 grid auto-rows-[minmax(150px,auto)] gap-4 sm:grid-cols-2 lg:grid-cols-3">
          <Bento icon={FileLock2} title="Your files stay private" body="Raw documents never leave the backend or reach the model — only the retrieved passages do." wide />
          <Bento icon={Users} title="Channels for a class" body="Curate a document set, approve members, and let everyone ask." />
          <Bento icon={MessagesSquare} title="Real-time messaging" body="Group rooms and DMs, with the AI one @ai away." />
          <Bento icon={BookOpen} title="Answers with footnotes" body="Citations to the exact source label — never the raw filename." />
          <Bento icon={Moon} title="Light & dark, warm either way" body="A paper feel by day, a paper-at-night feel after hours." />
          <Bento icon={Zap} title="Fast, focused answers" body="Retrieval-augmented, so responses stay tight and on-topic." wide />
        </div>
      </section>

      {/* CTA */}
      <section className="mx-auto max-w-3xl px-6 pb-24 text-center">
        <motion.div {...reveal}>
          <h2 className="font-display text-3xl">Start your study space</h2>
          <p className="mx-auto mt-3 max-w-md text-text-muted">
            Free to try. Add your first document and ask a question in under a minute.
          </p>
          <Link to="/signup" className="mt-6 inline-block"><Button size="lg">Get started</Button></Link>
        </motion.div>
      </section>

      {/* footer */}
      <footer className="border-t border-border">
        <div className="mx-auto flex max-w-6xl flex-col items-center justify-between gap-3 px-6 py-8 text-sm text-text-faint sm:flex-row">
          <span className="font-display text-lg text-primary">TAssist</span>
          <div className="flex gap-5">
            <span className="hover:text-text-muted">Privacy</span>
            <span className="hover:text-text-muted">Terms</span>
          </div>
          <span>© {new Date().getFullYear()} TAssist</span>
        </div>
      </footer>
    </div>
  )
}

function Bento({ icon: Icon, title, body, wide }: {
  icon: typeof BookOpen; title: string; body: string; wide?: boolean
}) {
  return (
    <motion.div {...reveal}
      className={`flex flex-col rounded-lg border border-border bg-bg-elev p-6 shadow-1 transition-all hover:-translate-y-1 hover:border-primary/40 hover:shadow-2 ${wide ? 'lg:col-span-2' : ''}`}>
      <div className="grid h-11 w-11 place-items-center rounded-md bg-primary-wash text-primary">
        <Icon size={20} strokeWidth={1.8} />
      </div>
      <h3 className="mt-4 font-display text-lg">{title}</h3>
      <p className="mt-1 text-sm text-text-muted">{body}</p>
    </motion.div>
  )
}
